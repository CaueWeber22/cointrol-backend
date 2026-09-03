package com.fcproject.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fcproject.application.core.domain.auth.AuthenticatedUser;
import com.fcproject.application.ports.outbound.AccessTokenOutPort;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class JwtTokenAdapter implements AccessTokenOutPort {
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private final String activeKeyId;
    private final Algorithm activeAlgorithm;
    private final Map<String, JWTVerifier> verifiers;
    private final String issuer;
    private final String audience;
    private final long accessTokenExpirationMinutes;

    public JwtTokenAdapter(
            String activeKeyId,
            String secret,
            String previousKeys,
            String issuer,
            String audience,
            long accessTokenExpirationMinutes
    ) {
        validateKeyId(activeKeyId);
        validateSecret(secret);
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("JWT issuer and audience must be configured");
        }
        if (accessTokenExpirationMinutes <= 0 || accessTokenExpirationMinutes > 60) {
            throw new IllegalArgumentException("JWT expiration must be between 1 and 60 minutes");
        }

        this.activeKeyId = activeKeyId;
        this.activeAlgorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer.strip();
        this.audience = audience.strip();
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.verifiers = buildVerifiers(activeKeyId, activeAlgorithm, previousKeys);
    }

    @Override
    public String generate(AuthenticatedUser user, Instant issuedAt) {
        return JWT.create()
                .withKeyId(activeKeyId)
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject(user.id().toString())
                .withClaim("email", user.email())
                .withClaim("roles", user.roles().stream().sorted().toList())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(issuedAt)
                .withExpiresAt(issuedAt.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES))
                .sign(activeAlgorithm);
    }

    @Override
    public long expirationSeconds() {
        return accessTokenExpirationMinutes * 60;
    }

    public DecodedJWT validate(String token) {
        DecodedJWT decoded = JWT.decode(token);
        String keyId = decoded.getKeyId();
        if (keyId == null) {
            return validateLegacyToken(decoded);
        }
        JWTVerifier verifier = verifiers.get(keyId);
        if (verifier == null) {
            throw new JWTVerificationException("Unknown JWT key id");
        }
        return verifier.verify(decoded);
    }

    private DecodedJWT validateLegacyToken(DecodedJWT decoded) {
        JWTVerificationException lastFailure = null;
        for (JWTVerifier verifier : verifiers.values()) {
            try {
                return verifier.verify(decoded);
            } catch (JWTVerificationException exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure == null
                ? new JWTVerificationException("No JWT verification key is configured")
                : lastFailure;
    }

    private Map<String, JWTVerifier> buildVerifiers(
            String activeKeyId,
            Algorithm activeAlgorithm,
            String previousKeys
    ) {
        Map<String, JWTVerifier> configured = new HashMap<>();
        configured.put(activeKeyId, verifier(activeAlgorithm));

        if (previousKeys != null && !previousKeys.isBlank()) {
            for (String entry : previousKeys.split(",")) {
                String normalized = entry.strip();
                int separator = normalized.indexOf(':');
                if (separator <= 0 || separator == normalized.length() - 1) {
                    throw new IllegalArgumentException("JWT previous keys must use the kid:secret format");
                }
                String keyId = normalized.substring(0, separator).strip();
                String previousSecret = normalized.substring(separator + 1).strip();
                validateKeyId(keyId);
                validateSecret(previousSecret);
                if (configured.putIfAbsent(keyId, verifier(Algorithm.HMAC256(previousSecret))) != null) {
                    throw new IllegalArgumentException("JWT key ids must be unique");
                }
            }
        }
        return Map.copyOf(configured);
    }

    private JWTVerifier verifier(Algorithm algorithm) {
        return JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build();
    }

    private void validateKeyId(String keyId) {
        if (keyId == null || !KEY_ID_PATTERN.matcher(keyId).matches()) {
            throw new IllegalArgumentException("JWT key id must contain 1 to 64 safe characters");
        }
    }

    private void validateSecret(String value) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > 512) {
            throw new IllegalArgumentException("JWT secret must contain at most 512 bytes");
        }
    }
}
