package com.fcproject.adapters.outbound.persistence.finance;

import com.fcproject.adapters.outbound.entities.finance.FinancialEntryEntity;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryStatus;
import com.fcproject.application.core.domain.finance.FinanceModels.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialEntryJPARepository extends JpaRepository<FinancialEntryEntity, UUID> {
    Optional<FinancialEntryEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<FinancialEntryEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    List<FinancialEntryEntity> findAllByUserIdAndAccountIdOrderByEffectiveDateAscCreatedAtAscIdAsc(
            UUID userId,
            UUID accountId
    );

    List<FinancialEntryEntity> findAllByUserIdAndTransferGroupId(UUID userId, UUID transferGroupId);

    List<FinancialEntryEntity> findAllByUserIdAndStatusAndEffectiveDateBetween(
            UUID userId,
            EntryStatus status,
            LocalDate from,
            LocalDate to
    );

    @Query("""
            select e from FinancialEntryEntity e
            where e.userId = :userId
              and (:accountId is null or e.accountId = :accountId)
              and (:categoryId is null or e.categoryId = :categoryId)
              and (:type is null or e.type = :type)
              and (:status is null or e.status = :status)
              and (:fromDate is null or e.effectiveDate >= :fromDate)
              and (:toDate is null or e.effectiveDate <= :toDate)
            """)
    Page<FinancialEntryEntity> search(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("type") EntryType type,
            @Param("status") EntryStatus status,
            @Param("fromDate") LocalDate from,
            @Param("toDate") LocalDate to,
            Pageable pageable
    );
}
