package com.fcproject.application.core.services;

import com.fcproject.application.core.domain.auth.AuthenticatedUser;
import com.fcproject.application.core.domain.auth.IssuedTokens;
import com.fcproject.application.core.domain.auth.StoredRefreshToken;
import com.fcproject.application.core.exceptions.InvalidRefreshTokenException;
import com.fcproject.application.ports.inbound.AuthInPort;
import com.fcproject.application.ports.outbound.AccessTokenOutPort;
import com.fcproject.application.ports.outbound.AuthenticationOutPort;
import com.fcproject.application.ports.outbound.RefreshTokenOutPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

import static com.fcproject.application.core.utils.UserValidationUtil.normalizeEmail;

public class AuthService implements AuthInPort {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationOutPort authentication;
    private final RefreshTokenOutPort refreshTokens;
    private final AccessTokenOutPort accessTokens;
    private final Clock clock;
    private final long refreshTokenExpirationDays;

    public AuthService(
            AuthenticationOutPort authentication,
            RefreshTokenOutPort refreshTokens,
            AccessTokenOutPort accessTokens,
            Clock clock,
            long refreshTokenExpirationDays
    ) {
        this.authentication = authentication;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.clock = clock;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Override
    public IssuedTokens login(String email, String password) {
        AuthenticatedUser user = authentication.authenticate(normalizeEmail(email), password);
        Instant now = clock.instant();
        String refreshToken = generateRefreshToken();

        refreshTokens.save(
                user.id(),
                hashToken(refreshToken),
                now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS)
        );

        return responseFor(user, refreshToken, now);
    }

    @Override
    public IssuedTokens refresh(String refreshToken) {
        String currentHash = hashToken(refreshToken);
        StoredRefreshToken storedToken = refreshTokens.findActiveByHash(currentHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        Instant now = clock.instant();
        if (storedToken.isExpiredAt(now)) {
            refreshTokens.revoke(storedToken.id(), now);
            throw new InvalidRefreshTokenException("Expired refresh token");
        }

        AuthenticatedUser user = authentication.loadById(storedToken.userId());
        String newRefreshToken = generateRefreshToken();
        refreshTokens.rotate(
                storedToken.id(),
                user.id(),
                hashToken(newRefreshToken),
                now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS),
                now
        );

        return responseFor(user, newRefreshToken, now);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokens.revokeByHash(hashToken(refreshToken), clock.instant());
    }

    private IssuedTokens responseFor(AuthenticatedUser user, String refreshToken, Instant issuedAt) {
        return new IssuedTokens(
                accessTokens.generate(user, issuedAt),
                refreshToken,
                accessTokens.expirationSeconds(),
                TOKEN_TYPE
        );
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token must be provided");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
