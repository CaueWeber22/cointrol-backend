package com.fcproject.application.ports.outbound;

import com.fcproject.application.core.domain.auth.LoginAttemptState;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface LoginAttemptOutPort {
    Optional<LoginAttemptState> find(String identifierHash);

    LoginAttemptState recordFailure(
            String identifierHash,
            Instant occurredAt,
            int maximumAttempts,
            Duration attemptWindow,
            Duration lockDuration
    );

    void clear(String identifierHash);
}
