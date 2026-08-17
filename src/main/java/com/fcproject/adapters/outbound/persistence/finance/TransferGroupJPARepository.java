package com.fcproject.adapters.outbound.persistence.finance;

import com.fcproject.adapters.outbound.entities.finance.TransferGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferGroupJPARepository extends JpaRepository<TransferGroupEntity, UUID> {
    Optional<TransferGroupEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
