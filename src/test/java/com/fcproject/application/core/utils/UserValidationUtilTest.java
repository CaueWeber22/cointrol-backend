package com.fcproject.application.core.utils;

import com.fcproject.application.core.exceptions.InvalidValueException;
import com.fcproject.application.core.exceptions.RequiredFieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UserValidationUtilTest {

    @Test
    void normalizesAValidEmail() {
        assertEquals("user@example.com", UserValidationUtil.normalizeEmail("  User@Example.COM "));
    }

    @Test
    void rejectsBlankAndMalformedEmails() {
        assertThrows(RequiredFieldException.class, () -> UserValidationUtil.normalizeEmail(" "));
        assertThrows(InvalidValueException.class, () -> UserValidationUtil.normalizeEmail("invalid"));
    }

    @Test
    void acceptsACompliantPassword() {
        assertDoesNotThrow(() -> UserValidationUtil.passwordValidationUtil("Valid@123"));
    }

    @Test
    void rejectsWeakPasswordsAndSpaces() {
        assertThrows(InvalidValueException.class, () -> UserValidationUtil.passwordValidationUtil("weakpass"));
        assertThrows(InvalidValueException.class, () -> UserValidationUtil.passwordValidationUtil("Valid @123"));
    }
}
