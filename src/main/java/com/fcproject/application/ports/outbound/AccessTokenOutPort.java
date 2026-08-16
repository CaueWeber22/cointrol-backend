package com.fcproject.application.ports.outbound;

import com.fcproject.application.core.domain.auth.AuthenticatedUser;

import java.time.Instant;

public interface AccessTokenOutPort {
    String generate(AuthenticatedUser user, Instant issuedAt);

    long expirationSeconds();
}
