package com.fcproject.application.core.domain.auth;

import java.time.Instant;
import java.util.UUID;

public record StoredRefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt
) {
    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
