package com.fcproject.application.core.domain.auth;

import java.time.Instant;

public record LoginAttemptState(int failedAttempts, Instant lockedUntil) {
    public boolean isLockedAt(Instant instant) {
        return lockedUntil != null && lockedUntil.isAfter(instant);
    }
}
