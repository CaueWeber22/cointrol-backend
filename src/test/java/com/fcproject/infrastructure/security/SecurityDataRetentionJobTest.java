package com.fcproject.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityDataRetentionJobTest {
    @Test
    void removesExpiredLoginAttemptsAndAuditEvents() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Instant now = Instant.parse("2026-08-23T12:00:00Z");
        SecurityDataRetentionJob job = new SecurityDataRetentionJob(
                jdbc,
                Clock.fixed(now, ZoneOffset.UTC),
                365,
                30
        );

        job.cleanExpiredSecurityData();

        verify(jdbc).update(
                "DELETE FROM access.login_attempts WHERE updated_at < ?",
                Timestamp.from(Instant.parse("2026-07-24T12:00:00Z"))
        );
        verify(jdbc).update(
                "DELETE FROM access.security_audit_events WHERE occurred_at < ?",
                Timestamp.from(Instant.parse("2025-08-23T12:00:00Z"))
        );
    }
}
