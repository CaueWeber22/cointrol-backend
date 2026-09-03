package com.fcproject.application.core.domain.auth;

public enum SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGIN_BLOCKED,
    TOKEN_REFRESH_SUCCESS,
    TOKEN_REFRESH_FAILURE,
    LOGOUT,
    RATE_LIMIT_EXCEEDED
}
