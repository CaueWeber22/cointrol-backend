package com.fcproject.application.core.domain.users;

import com.fcproject.application.core.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

public class UserDomain {
    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final Gender gender;
    private final LocalDate dateOfBirth;

    public UserDomain(UUID id, String firstName, String lastName, String email, String phone, Gender gender, LocalDate dateOfBirth) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

}
