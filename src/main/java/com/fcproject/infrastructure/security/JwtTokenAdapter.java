package com.fcproject.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fcproject.application.core.domain.auth.AuthenticatedUser;
import com.fcproject.application.ports.outbound.AccessTokenOutPort;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class JwtTokenAdapter implements AccessTokenOutPort {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final String audience;
    private final long accessTokenExpirationMinutes;

    public JwtTokenAdapter(
            String secret,
            String issuer,
            String audience,
            long accessTokenExpirationMinutes
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("JWT issuer and audience must be configured");
        }
        if (accessTokenExpirationMinutes <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }

        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build();
    }

    @Override
    public String generate(AuthenticatedUser user, Instant issuedAt) {
        return JWT.create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject(user.id().toString())
                .withClaim("email", user.email())
                .withClaim("roles", user.roles().stream().sorted().toList())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(issuedAt)
                .withExpiresAt(issuedAt.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES))
                .sign(algorithm);
    }

    @Override
    public long expirationSeconds() {
        return accessTokenExpirationMinutes * 60;
    }

    public DecodedJWT validate(String token) {
        return verifier.verify(token);
    }
}
