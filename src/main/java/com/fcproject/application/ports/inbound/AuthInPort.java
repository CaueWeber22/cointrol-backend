package com.fcproject.application.ports.inbound;

import com.fcproject.application.core.domain.auth.IssuedTokens;
import com.fcproject.application.core.domain.auth.AuthRequestContext;

public interface AuthInPort {
    IssuedTokens login(String email, String password, AuthRequestContext context);

    IssuedTokens refresh(String refreshToken, AuthRequestContext context);

    void logout(String refreshToken, AuthRequestContext context);
}
