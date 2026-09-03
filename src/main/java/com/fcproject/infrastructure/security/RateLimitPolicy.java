package com.fcproject.infrastructure.security;

import java.time.Duration;

public record RateLimitPolicy(String name, int requestLimit, Duration window) {
    public RateLimitPolicy {
        if (name == null || name.isBlank() || requestLimit <= 0 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit policy is invalid");
        }
    }
}
