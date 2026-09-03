package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.request.LoginCredentialsRequest;
import com.fcproject.adapters.inbound.dto.request.RefreshTokenRequest;
import com.fcproject.adapters.inbound.dto.response.TokenResponse;
import com.fcproject.application.core.domain.auth.AuthRequestContext;
import com.fcproject.application.ports.inbound.AuthInPort;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthInPort auth;

    public AuthController(AuthInPort auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginCredentialsRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(TokenResponse.from(auth.login(
                request.email(), request.password(), context(servletRequest)
        )));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(TokenResponse.from(auth.refresh(
                request.refreshToken(), context(servletRequest)
        )));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        auth.logout(request.refreshToken(), context(servletRequest));
        return ResponseEntity.noContent().build();
    }

    private AuthRequestContext context(HttpServletRequest request) {
        return new AuthRequestContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
