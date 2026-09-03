package com.fcproject.application.core.exceptions;

public class LoginBlockedException extends RuntimeException {
    private final long retryAfterSeconds;

    public LoginBlockedException(long retryAfterSeconds) {
        super("Too many failed login attempts");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
