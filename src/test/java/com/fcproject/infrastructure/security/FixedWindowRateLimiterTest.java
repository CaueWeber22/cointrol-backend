package com.fcproject.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedWindowRateLimiterTest {
    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(100);
    private final RateLimitPolicy policy = new RateLimitPolicy("login", 2, Duration.ofSeconds(60));

    @Test
    void rejectsRequestsOverTheLimitAndReturnsRetryAfter() {
        Instant now = Instant.parse("2026-08-23T12:00:10Z");

        assertTrue(limiter.acquire("login:127.0.0.1", policy, now).allowed());
        assertTrue(limiter.acquire("login:127.0.0.1", policy, now).allowed());
        var rejected = limiter.acquire("login:127.0.0.1", policy, now);

        assertFalse(rejected.allowed());
        assertTrue(rejected.firstRejection());
        assertEquals(50, rejected.retryAfterSeconds());
    }

    @Test
    void startsANewCounterInTheNextWindow() {
        Instant firstWindow = Instant.parse("2026-08-23T12:00:10Z");
        limiter.acquire("login:127.0.0.1", policy, firstWindow);
        limiter.acquire("login:127.0.0.1", policy, firstWindow);
        assertFalse(limiter.acquire("login:127.0.0.1", policy, firstWindow).allowed());

        assertTrue(limiter.acquire(
                "login:127.0.0.1",
                policy,
                Instant.parse("2026-08-23T12:01:00Z")
        ).allowed());
    }
}
