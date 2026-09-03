package com.fcproject.adapters.outbound.entities.auth;

import com.fcproject.application.core.domain.auth.SecurityEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "access", name = "security_audit_events")
public class SecurityAuditEventEntity {
    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "identifier_hash", length = 64)
    private String identifierHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private SecurityEventType eventType;

    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SecurityAuditEventEntity() {
    }

    public SecurityAuditEventEntity(
            UUID id,
            UUID userId,
            String identifierHash,
            SecurityEventType eventType,
            String clientIp,
            String userAgent,
            Instant occurredAt
    ) {
        this.id = id;
        this.userId = userId;
        this.identifierHash = identifierHash;
        this.eventType = eventType;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.occurredAt = occurredAt;
    }
}
