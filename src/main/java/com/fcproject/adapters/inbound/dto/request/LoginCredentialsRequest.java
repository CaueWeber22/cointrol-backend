package com.fcproject.adapters.inbound.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginCredentialsRequest(
        @NotBlank @Email @Size(max = 250) String email,
        @NotBlank @Size(max = 72) String password
) {
}
