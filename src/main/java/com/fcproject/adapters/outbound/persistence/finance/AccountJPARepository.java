package com.fcproject.adapters.outbound.persistence.finance;

import com.fcproject.adapters.outbound.entities.finance.AccountEntity;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountJPARepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select a from AccountEntity a
            where a.userId = :userId and (:status is null or a.status = :status)
            order by lower(a.name), a.id
            """)
    List<AccountEntity> findOwned(@Param("userId") UUID userId, @Param("status") ResourceStatus status);

    @Query("""
            select count(a) > 0 from AccountEntity a
            where a.userId = :userId
              and a.status = :activeStatus
              and lower(a.name) = lower(:name)
              and (:ignoredId is null or a.id <> :ignoredId)
            """)
    boolean existsActiveName(
            @Param("userId") UUID userId,
            @Param("name") String name,
            @Param("activeStatus") ResourceStatus activeStatus,
            @Param("ignoredId") UUID ignoredId
    );
}
