package com.fcproject.application.core.domain.auth;

import java.time.Instant;
import java.util.UUID;

public record SecurityAuditEvent(
        UUID id,
        UUID userId,
        String identifierHash,
        SecurityEventType eventType,
        String clientIp,
        String userAgent,
        Instant occurredAt
) {
}
