package com.fcproject.application.core.utils;

import com.fcproject.infrastructure.exceptions.global.InvalidValueException;
import com.fcproject.infrastructure.exceptions.global.NotAllFieldsFilledException;

import java.util.regex.Pattern;

public final class UserValidationUtil {

    public static void emailValidationUtil(String email) {
        if (email == null || email.isBlank()){
            throw new NotAllFieldsFilledException("Email field must be filled");
        }

        String emailRegex = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";
        Pattern pattern = Pattern.compile(emailRegex, Pattern.CASE_INSENSITIVE);
        if (!pattern.matcher(email).matches()) {
            throw new InvalidValueException("Invalid email format");
        }
    }

    public static void passwordValidationUtil(String password) {
        if (password == null || password.isBlank()){
            throw new NotAllFieldsFilledException("Password field must be filled");
        }

        if(password.contains(" ")){
            throw new InvalidValueException("The password must not contain spaces");
        }

        String passwordRegex ="^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
        Pattern pattern = Pattern.compile(passwordRegex);

        if (!pattern.matcher(password).matches()) {
            throw new InvalidValueException("The password must be at least 8 characters and contain at least one uppercase, one special character and one lowercase letter");
        }
    }
}
