package com.fcproject.adapters.outbound.persistence.finance;

import com.fcproject.adapters.outbound.entities.finance.CategoryEntity;
import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryJPARepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select c from CategoryEntity c
            where c.userId = :userId
              and (:kind is null or c.kind = :kind)
              and (:status is null or c.status = :status)
            order by c.kind, lower(c.name), c.id
            """)
    List<CategoryEntity> findOwned(
            @Param("userId") UUID userId,
            @Param("kind") CategoryKind kind,
            @Param("status") ResourceStatus status
    );

    @Query("""
            select count(c) > 0 from CategoryEntity c
            where c.userId = :userId
              and c.kind = :kind
              and c.status = :activeStatus
              and lower(c.name) = lower(:name)
              and (:ignoredId is null or c.id <> :ignoredId)
            """)
    boolean existsActiveName(
            @Param("userId") UUID userId,
            @Param("kind") CategoryKind kind,
            @Param("name") String name,
            @Param("activeStatus") ResourceStatus activeStatus,
            @Param("ignoredId") UUID ignoredId
    );
}
