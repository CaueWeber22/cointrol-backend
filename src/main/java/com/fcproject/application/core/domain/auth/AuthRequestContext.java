package com.fcproject.application.core.domain.auth;

public record AuthRequestContext(String clientIp, String userAgent) {
    public static AuthRequestContext unknown() {
        return new AuthRequestContext("unknown", null);
    }
}
