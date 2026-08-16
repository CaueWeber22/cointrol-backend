package com.fcproject.application.ports.outbound;

import com.fcproject.application.core.domain.auth.AuthenticatedUser;

import java.util.UUID;

public interface AuthenticationOutPort {
    AuthenticatedUser authenticate(String email, String rawPassword);

    AuthenticatedUser loadById(UUID userId);
}
