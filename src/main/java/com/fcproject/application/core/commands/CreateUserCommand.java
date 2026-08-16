package com.fcproject.application.core.commands;

import com.fcproject.application.core.enums.Gender;

import java.time.LocalDate;

public record CreateUserCommand(
        String firstName,
        String lastName,
        String email,
        String phone,
        Gender gender,
        LocalDate dateOfBirth,
        String password
) {
}
