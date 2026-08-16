package com.fcproject.adapters.inbound.dto.response;

import com.fcproject.application.core.domain.auth.IssuedTokens;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String type
) {
    public static TokenResponse from(IssuedTokens tokens) {
        return new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn(),
                tokens.type()
        );
    }
}
