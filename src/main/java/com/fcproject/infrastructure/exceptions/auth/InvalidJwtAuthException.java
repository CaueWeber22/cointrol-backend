package com.fcproject.infrastructure.exceptions.auth;

public class InvalidJwtAuthException extends RuntimeException {
    public InvalidJwtAuthException(String message) {
        super(message);
    }
}
