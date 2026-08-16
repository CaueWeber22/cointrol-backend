package com.fcproject.application.ports.inbound;

import com.fcproject.application.core.domain.auth.IssuedTokens;

public interface AuthInPort {
    IssuedTokens login(String email, String password);

    IssuedTokens refresh(String refreshToken);

    void logout(String refreshToken);
}
