package com.fcproject.application.core.domain.auth;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, Set<String> roles) {
    public AuthenticatedUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
