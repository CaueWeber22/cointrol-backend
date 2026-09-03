package com.fcproject.application.core.services;

import com.fcproject.application.core.domain.auth.AuthenticatedUser;
import com.fcproject.application.core.domain.auth.AuthRequestContext;
import com.fcproject.application.core.domain.auth.IssuedTokens;
import com.fcproject.application.core.domain.auth.LoginAttemptState;
import com.fcproject.application.core.domain.auth.SecurityAuditEvent;
import com.fcproject.application.core.domain.auth.SecurityEventType;
import com.fcproject.application.core.domain.auth.StoredRefreshToken;
import com.fcproject.application.core.exceptions.InvalidCredentialsException;
import com.fcproject.application.core.exceptions.InvalidRefreshTokenException;
import com.fcproject.application.core.exceptions.LoginBlockedException;
import com.fcproject.application.ports.inbound.AuthInPort;
import com.fcproject.application.ports.outbound.AccessTokenOutPort;
import com.fcproject.application.ports.outbound.AuthenticationOutPort;
import com.fcproject.application.ports.outbound.LoginAttemptOutPort;
import com.fcproject.application.ports.outbound.RefreshTokenOutPort;
import com.fcproject.application.ports.outbound.SecurityAuditOutPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
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
    private final LoginAttemptOutPort loginAttempts;
    private final SecurityAuditOutPort securityAudit;
    private final Clock clock;
    private final long refreshTokenExpirationDays;
    private final int maximumLoginAttempts;
    private final Duration loginAttemptWindow;
    private final Duration loginLockDuration;

    public AuthService(
            AuthenticationOutPort authentication,
            RefreshTokenOutPort refreshTokens,
            AccessTokenOutPort accessTokens,
            LoginAttemptOutPort loginAttempts,
            SecurityAuditOutPort securityAudit,
            Clock clock,
            long refreshTokenExpirationDays,
            int maximumLoginAttempts,
            Duration loginAttemptWindow,
            Duration loginLockDuration
    ) {
        if (refreshTokenExpirationDays <= 0 || refreshTokenExpirationDays > 90) {
            throw new IllegalArgumentException("Refresh token expiration must be between 1 and 90 days");
        }
        if (maximumLoginAttempts < 2 || loginAttemptWindow.isNegative() || loginAttemptWindow.isZero()
                || loginLockDuration.isNegative() || loginLockDuration.isZero()) {
            throw new IllegalArgumentException("Login protection configuration is invalid");
        }
        this.authentication = authentication;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.loginAttempts = loginAttempts;
        this.securityAudit = securityAudit;
        this.clock = clock;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.maximumLoginAttempts = maximumLoginAttempts;
        this.loginAttemptWindow = loginAttemptWindow;
        this.loginLockDuration = loginLockDuration;
    }

    @Override
    public IssuedTokens login(String email, String password, AuthRequestContext context) {
        String normalizedEmail = normalizeEmail(email);
        String identifierHash = hashValue(normalizedEmail);
        AuthRequestContext normalizedContext = normalizeContext(context);
        Instant now = clock.instant();

        loginAttempts.find(identifierHash)
                .filter(state -> state.isLockedAt(now))
                .ifPresent(state -> {
                    audit(null, identifierHash, SecurityEventType.LOGIN_BLOCKED, normalizedContext, now);
                    throw blocked(state, now);
                });

        AuthenticatedUser user;
        try {
            user = authentication.authenticate(normalizedEmail, password);
        } catch (InvalidCredentialsException exception) {
            LoginAttemptState state = loginAttempts.recordFailure(
                    identifierHash,
                    now,
                    maximumLoginAttempts,
                    loginAttemptWindow,
                    loginLockDuration
            );
            SecurityEventType eventType = state.isLockedAt(now)
                    ? SecurityEventType.LOGIN_BLOCKED
                    : SecurityEventType.LOGIN_FAILURE;
            audit(null, identifierHash, eventType, normalizedContext, now);
            throw exception;
        }

        String refreshToken = generateRefreshToken();
        refreshTokens.save(
                user.id(),
                hashToken(refreshToken),
                now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS)
        );
        IssuedTokens issuedTokens = responseFor(user, refreshToken, now);
        loginAttempts.clear(identifierHash);
        audit(user.id(), identifierHash, SecurityEventType.LOGIN_SUCCESS, normalizedContext, now);

        return issuedTokens;
    }

    @Override
    public IssuedTokens refresh(String refreshToken, AuthRequestContext context) {
        AuthRequestContext normalizedContext = normalizeContext(context);
        Instant now = clock.instant();
        try {
            String currentHash = hashToken(refreshToken);
            StoredRefreshToken storedToken = refreshTokens.findActiveByHash(currentHash)
                    .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

            if (storedToken.isExpiredAt(now)) {
                refreshTokens.revoke(storedToken.id(), now);
                throw new InvalidRefreshTokenException("Expired refresh token");
            }

            AuthenticatedUser user;
            try {
                user = authentication.loadById(storedToken.userId());
            } catch (InvalidCredentialsException exception) {
                refreshTokens.revoke(storedToken.id(), now);
                throw new InvalidRefreshTokenException("Invalid refresh token");
            }
            String newRefreshToken = generateRefreshToken();
            refreshTokens.rotate(
                    storedToken.id(),
                    user.id(),
                    hashToken(newRefreshToken),
                    now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS),
                    now
            );
            IssuedTokens issuedTokens = responseFor(user, newRefreshToken, now);
            audit(user.id(), null, SecurityEventType.TOKEN_REFRESH_SUCCESS, normalizedContext, now);

            return issuedTokens;
        } catch (InvalidRefreshTokenException exception) {
            audit(null, null, SecurityEventType.TOKEN_REFRESH_FAILURE, normalizedContext, now);
            throw exception;
        }
    }

    @Override
    public void logout(String refreshToken, AuthRequestContext context) {
        Instant now = clock.instant();
        refreshTokens.revokeByHash(hashToken(refreshToken), now);
        audit(null, null, SecurityEventType.LOGOUT, normalizeContext(context), now);
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

    private String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private LoginBlockedException blocked(LoginAttemptState state, Instant now) {
        return new LoginBlockedException(Duration.between(now, state.lockedUntil()).toSeconds());
    }

    private AuthRequestContext normalizeContext(AuthRequestContext context) {
        AuthRequestContext source = context == null ? AuthRequestContext.unknown() : context;
        return new AuthRequestContext(
                limited(source.clientIp(), 45, "unknown"),
                limited(source.userAgent(), 255, null)
        );
    }

    private String limited(String value, int maximumLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.strip();
        return normalized.length() <= maximumLength ? normalized : normalized.substring(0, maximumLength);
    }

    private void audit(
            java.util.UUID userId,
            String identifierHash,
            SecurityEventType eventType,
            AuthRequestContext context,
            Instant occurredAt
    ) {
        securityAudit.record(new SecurityAuditEvent(
                java.util.UUID.randomUUID(),
                userId,
                identifierHash,
                eventType,
                context.clientIp(),
                context.userAgent(),
                occurredAt
        ));
    }
}
