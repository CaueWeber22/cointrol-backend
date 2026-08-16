package com.fcproject.application.core.utils;

import com.fcproject.application.core.exceptions.InvalidValueException;
import com.fcproject.application.core.exceptions.RequiredFieldException;

import java.util.regex.Pattern;

public final class UserValidationUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,72}$"
    );

    private UserValidationUtil() {
    }

    public static void emailValidationUtil(String email) {
        if (email == null || email.isBlank()){
            throw new RequiredFieldException("Email field must be filled");
        }

        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new InvalidValueException("Invalid email format");
        }
    }

    public static void passwordValidationUtil(String password) {
        if (password == null || password.isBlank()){
            throw new RequiredFieldException("Password field must be filled");
        }

        if(password.contains(" ")){
            throw new InvalidValueException("The password must not contain spaces");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidValueException("The password must contain 8 to 72 characters, including uppercase, lowercase, number and special character");
        }
    }

    public static String normalizeEmail(String email) {
        emailValidationUtil(email);
        return email.trim().toLowerCase();
    }
}
