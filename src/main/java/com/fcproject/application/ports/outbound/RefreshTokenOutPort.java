package com.fcproject.application.ports.outbound;

import com.fcproject.application.core.domain.auth.StoredRefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenOutPort {
    Optional<StoredRefreshToken> findActiveByHash(String tokenHash);

    void save(UUID userId, String tokenHash, Instant expiresAt);

    void rotate(UUID currentTokenId, UUID userId, String newTokenHash, Instant newExpiresAt, Instant revokedAt);

    void revoke(UUID tokenId, Instant revokedAt);

    void revokeByHash(String tokenHash, Instant revokedAt);
}
