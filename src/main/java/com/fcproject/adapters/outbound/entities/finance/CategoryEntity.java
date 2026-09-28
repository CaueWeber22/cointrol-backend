package com.fcproject.adapters.outbound.entities.finance;

import com.fcproject.application.core.domain.finance.FinanceModels.CategoryKind;
import com.fcproject.application.core.domain.finance.FinanceModels.CategorySource;
import com.fcproject.application.core.domain.finance.FinanceModels.DefaultCategoryKey;
import com.fcproject.application.core.domain.finance.FinanceModels.ResourceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "finance", name = "categories")
public class CategoryEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResourceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategorySource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_key", length = 50)
    private DefaultCategoryKey defaultKey;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CategoryEntity() {
    }

    public CategoryEntity(
            UUID id, UUID userId, String name, CategoryKind kind, ResourceStatus status,
            long version, Instant createdAt, Instant updatedAt
    ) {
        this(id, userId, name, kind, status, CategorySource.CUSTOM, null, version, createdAt, updatedAt);
    }

    public CategoryEntity(
            UUID id, UUID userId, String name, CategoryKind kind, ResourceStatus status,
            CategorySource source, DefaultCategoryKey defaultKey, long version, Instant createdAt, Instant updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.kind = kind;
        this.status = status;
        this.source = source;
        this.defaultKey = defaultKey;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public CategoryKind getKind() { return kind; }
    public ResourceStatus getStatus() { return status; }
    public CategorySource getSource() { return source; }
    public DefaultCategoryKey getDefaultKey() { return defaultKey; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
