package com.fcproject.adapters.inbound.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserCreationRequest(
        @JsonProperty("first_name") @NotBlank @Size(max = 50) String firstName,
        @JsonProperty("last_name") @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Email @Size(max = 250) String email,
        @NotBlank @Pattern(regexp = "^[0-9+()\\-\\s]{8,20}$") String phone,
        @NotBlank @Size(max = 10) String gender,
        @JsonProperty("date_of_birth") @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
