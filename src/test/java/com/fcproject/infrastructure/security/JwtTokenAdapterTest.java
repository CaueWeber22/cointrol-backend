package com.fcproject.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fcproject.application.core.domain.auth.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenAdapterTest {
    private static final String OLD_SECRET = "old-secret-with-at-least-thirty-two-bytes-2026";
    private static final String CURRENT_SECRET = "current-secret-with-at-least-thirty-two-bytes-2026";
    private static final Instant NOW = Instant.now();
    private static final AuthenticatedUser USER = new AuthenticatedUser(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "user@example.com",
            Set.of("ROLE_USER")
    );

    @Test
    void signsWithTheActiveKeyId() {
        JwtTokenAdapter tokens = adapter("current", CURRENT_SECRET, "");

        String token = tokens.generate(USER, NOW);

        assertEquals("current", JWT.decode(token).getKeyId());
        assertEquals(USER.id().toString(), tokens.validate(token).getSubject());
    }

    @Test
    void validatesTokensSignedByAPreviousKeyDuringRotation() {
        String oldToken = adapter("old", OLD_SECRET, "").generate(USER, NOW);
        JwtTokenAdapter rotated = adapter("current", CURRENT_SECRET, "old:" + OLD_SECRET);

        assertEquals(USER.id().toString(), rotated.validate(oldToken).getSubject());
    }

    @Test
    void validatesLegacyTokensWithoutKeyIdAgainstPreviousKeys() {
        String legacyToken = JWT.create()
                .withIssuer("cointrol-api")
                .withAudience("cointrol-client")
                .withSubject(USER.id().toString())
                .withIssuedAt(NOW)
                .withExpiresAt(NOW.plusSeconds(900))
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256(OLD_SECRET));
        JwtTokenAdapter rotated = adapter("current", CURRENT_SECRET, "old:" + OLD_SECRET);

        assertEquals(USER.id().toString(), rotated.validate(legacyToken).getSubject());
    }

    @Test
    void rejectsUnknownKeyIdsAndWeakSecrets() {
        String unknownToken = adapter("unknown", OLD_SECRET, "").generate(USER, NOW);
        JwtTokenAdapter current = adapter("current", CURRENT_SECRET, "");

        assertThrows(JWTVerificationException.class, () -> current.validate(unknownToken));
        assertThrows(IllegalArgumentException.class, () ->
                adapter("current", "short", ""));
    }

    private JwtTokenAdapter adapter(String keyId, String secret, String previousKeys) {
        return new JwtTokenAdapter(
                keyId,
                secret,
                previousKeys,
                "cointrol-api",
                "cointrol-client",
                15
        );
    }
}
