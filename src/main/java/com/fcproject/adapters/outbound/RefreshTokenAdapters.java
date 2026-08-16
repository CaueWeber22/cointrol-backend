package com.fcproject.adapters.outbound;

import com.fcproject.adapters.outbound.entities.auth.RefreshTokenEntity;
import com.fcproject.adapters.outbound.entities.users.UserEntity;
import com.fcproject.adapters.outbound.persistence.RefreshTokenJPARepository;
import com.fcproject.adapters.outbound.persistence.UserJPARepository;
import com.fcproject.application.core.domain.auth.StoredRefreshToken;
import com.fcproject.application.core.exceptions.InvalidRefreshTokenException;
import com.fcproject.application.ports.outbound.RefreshTokenOutPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class RefreshTokenAdapters implements RefreshTokenOutPort {
    private final RefreshTokenJPARepository refreshTokenRepository;
    private final UserJPARepository userRepository;

    public RefreshTokenAdapters(
            RefreshTokenJPARepository refreshTokenRepository,
            UserJPARepository userRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredRefreshToken> findActiveByHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(UUID userId, String tokenHash, Instant expiresAt) {
        UserEntity user = userRepository.getReferenceById(userId);
        refreshTokenRepository.save(new RefreshTokenEntity(user, tokenHash, expiresAt));
    }

    @Override
    @Transactional
    public void rotate(
            UUID currentTokenId,
            UUID userId,
            String newTokenHash,
            Instant newExpiresAt,
            Instant revokedAt
    ) {
        RefreshTokenEntity current = refreshTokenRepository.findByIdForUpdate(currentTokenId)
                .filter(token -> token.getRevokedAt() == null)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token was already used"));
        current.revoke(revokedAt);

        UserEntity user = userRepository.getReferenceById(userId);
        refreshTokenRepository.save(new RefreshTokenEntity(user, newTokenHash, newExpiresAt));
    }

    @Override
    @Transactional
    public void revoke(UUID tokenId, Instant revokedAt) {
        refreshTokenRepository.findByIdForUpdate(tokenId)
                .ifPresent(token -> token.revoke(revokedAt));
    }

    @Override
    @Transactional
    public void revokeByHash(String tokenHash, Instant revokedAt) {
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.revoke(revokedAt));
    }

    private StoredRefreshToken toDomain(RefreshTokenEntity entity) {
        return new StoredRefreshToken(
                entity.getId(),
                entity.getUser().getId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getRevokedAt()
        );
    }
}
