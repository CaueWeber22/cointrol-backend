package com.fcproject.application.core.enums;

import com.fcproject.application.core.exceptions.InvalidValueException;

public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    public static Gender fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("Gender value cannot be null or blank");
        }

        String normalized = value.trim().toUpperCase();

        try {
            return Gender.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException(
                    "Invalid gender value. Allowed: MALE, FEMALE, OTHER"
            );
        }
    }
}
