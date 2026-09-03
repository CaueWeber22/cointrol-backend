package com.fcproject.adapters.outbound;

import com.fcproject.application.core.domain.auth.LoginAttemptState;
import com.fcproject.application.ports.outbound.LoginAttemptOutPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class LoginAttemptPersistenceAdapter implements LoginAttemptOutPort {
    private static final String FIND_SQL = """
            SELECT failed_attempts, locked_until
            FROM access.login_attempts
            WHERE identifier_hash = ?
            """;

    private static final String RECORD_FAILURE_SQL = """
            INSERT INTO access.login_attempts AS attempts (
                identifier_hash, failed_attempts, window_started_at, locked_until, last_failure_at, updated_at
            ) VALUES (?, 1, ?, NULL, ?, ?)
            ON CONFLICT (identifier_hash) DO UPDATE SET
                failed_attempts = CASE
                    WHEN attempts.window_started_at <= ?
                      OR (attempts.locked_until IS NOT NULL
                          AND attempts.locked_until <= ?)
                    THEN 1
                    ELSE attempts.failed_attempts + 1
                END,
                window_started_at = CASE
                    WHEN attempts.window_started_at <= ?
                      OR (attempts.locked_until IS NOT NULL
                          AND attempts.locked_until <= ?)
                    THEN EXCLUDED.window_started_at
                    ELSE attempts.window_started_at
                END,
                locked_until = CASE
                    WHEN attempts.window_started_at <= ?
                      OR (attempts.locked_until IS NOT NULL
                          AND attempts.locked_until <= ?)
                    THEN NULL
                    WHEN attempts.failed_attempts + 1 >= ? THEN ?
                    ELSE attempts.locked_until
                END,
                last_failure_at = EXCLUDED.last_failure_at,
                updated_at = EXCLUDED.updated_at
            RETURNING failed_attempts, locked_until
            """;

    private final JdbcTemplate jdbc;

    public LoginAttemptPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoginAttemptState> find(String identifierHash) {
        return jdbc.query(
                FIND_SQL,
                resultSet -> resultSet.next()
                        ? Optional.of(toState(resultSet.getInt("failed_attempts"), resultSet.getTimestamp("locked_until")))
                        : Optional.empty(),
                identifierHash
        );
    }

    @Override
    @Transactional
    public LoginAttemptState recordFailure(
            String identifierHash,
            Instant occurredAt,
            int maximumAttempts,
            Duration attemptWindow,
            Duration lockDuration
    ) {
        Instant windowThreshold = occurredAt.minus(attemptWindow);
        Instant lockedUntil = occurredAt.plus(lockDuration);
        return jdbc.queryForObject(
                RECORD_FAILURE_SQL,
                (resultSet, rowNumber) -> toState(
                        resultSet.getInt("failed_attempts"),
                        resultSet.getTimestamp("locked_until")
                ),
                identifierHash,
                Timestamp.from(occurredAt),
                Timestamp.from(occurredAt),
                Timestamp.from(occurredAt),
                Timestamp.from(windowThreshold),
                Timestamp.from(occurredAt),
                Timestamp.from(windowThreshold),
                Timestamp.from(occurredAt),
                Timestamp.from(windowThreshold),
                Timestamp.from(occurredAt),
                maximumAttempts,
                Timestamp.from(lockedUntil)
        );
    }

    @Override
    @Transactional
    public void clear(String identifierHash) {
        jdbc.update("DELETE FROM access.login_attempts WHERE identifier_hash = ?", identifierHash);
    }

    private LoginAttemptState toState(int failedAttempts, Timestamp lockedUntil) {
        return new LoginAttemptState(
                failedAttempts,
                lockedUntil == null ? null : lockedUntil.toInstant()
        );
    }
}
