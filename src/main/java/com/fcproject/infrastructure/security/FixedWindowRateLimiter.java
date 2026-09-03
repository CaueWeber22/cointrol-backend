package com.fcproject.infrastructure.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class FixedWindowRateLimiter {
    private static final long CLEANUP_INTERVAL = 512;

    private final Map<String, WindowState> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();
    private final int maximumTrackedKeys;

    public FixedWindowRateLimiter(int maximumTrackedKeys) {
        if (maximumTrackedKeys < 100) {
            throw new IllegalArgumentException("Rate limiter must track at least 100 keys");
        }
        this.maximumTrackedKeys = maximumTrackedKeys;
    }

    public RateLimitDecision acquire(String key, RateLimitPolicy policy, Instant now) {
        cleanupIfNecessary(now);
        long nowEpochSecond = now.getEpochSecond();
        long windowSeconds = policy.window().toSeconds();
        long windowEndsAt = Math.floorDiv(nowEpochSecond, windowSeconds) * windowSeconds + windowSeconds;
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();

        windows.compute(key, (ignored, current) -> {
            if (current == null || current.endsAtEpochSecond <= nowEpochSecond) {
                if (current == null && windows.size() >= maximumTrackedKeys) {
                    decision.set(RateLimitDecision.deny(Math.max(1, windowSeconds), false));
                    return null;
                }
                decision.set(RateLimitDecision.allow());
                return new WindowState(1, windowEndsAt);
            }

            current.requestCount++;
            if (current.requestCount <= policy.requestLimit()) {
                decision.set(RateLimitDecision.allow());
            } else {
                decision.set(RateLimitDecision.deny(
                        Math.max(1, current.endsAtEpochSecond - nowEpochSecond),
                        current.requestCount == policy.requestLimit() + 1
                ));
            }
            return current;
        });

        return decision.get();
    }

    private void cleanupIfNecessary(Instant now) {
        if (requestCount.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        long nowEpochSecond = now.getEpochSecond();
        windows.entrySet().removeIf(entry -> entry.getValue().endsAtEpochSecond <= nowEpochSecond);
    }

    private static final class WindowState {
        private int requestCount;
        private final long endsAtEpochSecond;

        private WindowState(int requestCount, long endsAtEpochSecond) {
            this.requestCount = requestCount;
            this.endsAtEpochSecond = endsAtEpochSecond;
        }
    }

    public record RateLimitDecision(boolean allowed, long retryAfterSeconds, boolean firstRejection) {
        private static RateLimitDecision allow() {
            return new RateLimitDecision(true, 0, false);
        }

        private static RateLimitDecision deny(long retryAfterSeconds, boolean firstRejection) {
            return new RateLimitDecision(false, retryAfterSeconds, firstRejection);
        }
    }
}
