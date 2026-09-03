package com.fcproject.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Component
public class SecurityDataRetentionJob {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final int auditRetentionDays;
    private final int loginAttemptRetentionDays;

    public SecurityDataRetentionJob(
            JdbcTemplate jdbc,
            Clock clock,
            @Value("${security.audit.retention-days}") int auditRetentionDays,
            @Value("${security.login-protection.retention-days}") int loginAttemptRetentionDays
    ) {
        if (auditRetentionDays < 30 || auditRetentionDays > 3650) {
            throw new IllegalArgumentException("Security audit retention must be between 30 and 3650 days");
        }
        if (loginAttemptRetentionDays < 1 || loginAttemptRetentionDays > 365) {
            throw new IllegalArgumentException("Login attempt retention must be between 1 and 365 days");
        }
        this.jdbc = jdbc;
        this.clock = clock;
        this.auditRetentionDays = auditRetentionDays;
        this.loginAttemptRetentionDays = loginAttemptRetentionDays;
    }

    @Scheduled(cron = "${security.audit.cleanup-cron}", zone = "UTC")
    @Transactional
    public void cleanExpiredSecurityData() {
        var now = clock.instant();
        jdbc.update(
                "DELETE FROM access.login_attempts WHERE updated_at < ?",
                Timestamp.from(now.minus(loginAttemptRetentionDays, ChronoUnit.DAYS))
        );
        jdbc.update(
                "DELETE FROM access.security_audit_events WHERE occurred_at < ?",
                Timestamp.from(now.minus(auditRetentionDays, ChronoUnit.DAYS))
        );
    }
}
