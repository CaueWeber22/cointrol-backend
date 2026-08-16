package com.fcproject.adapters.inbound.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fcproject.application.core.domain.users.UserDomain;

import java.time.LocalDate;
import java.util.UUID;

public record UserInfoResponse(
        UUID id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String email,
        String phone,
        String gender,
        @JsonProperty("date_of_birth") LocalDate dateOfBirth
) {
    public static UserInfoResponse from(UserDomain user) {
        return new UserInfoResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getGender().name(),
                user.getDateOfBirth()
        );
    }
}
