package com.fcproject.application.core.domain.auth;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String type,
        java.time.Instant issuedAt,
        java.time.Instant refreshExpiresAt
) {
}
