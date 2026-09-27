package com.fcproject.adapters.inbound.controllers;

import com.fcproject.adapters.inbound.dto.request.LoginCredentialsRequest;
import com.fcproject.application.core.domain.auth.AuthRequestContext;
import com.fcproject.application.ports.inbound.AuthInPort;
import com.fcproject.infrastructure.security.AuthCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthInPort auth;
    private final AuthCookies cookies;
    private final CsrfTokenRepository csrfTokens;

    public AuthController(AuthInPort auth, AuthCookies cookies, CsrfTokenRepository csrfTokens) {
        this.auth = auth;
        this.cookies = cookies;
        this.csrfTokens = csrfTokens;
    }

    public record CsrfResponse(String token, String headerName) {}

    @GetMapping("/csrf")
    @Operation(summary = "Obter CSRF da sessão; renovar após login/refresh", security = {})
    public CsrfResponse csrf(@io.swagger.v3.oas.annotations.Parameter(hidden = true) CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/login")
    @ApiResponse(responseCode = "204", description = "Cookies HttpOnly emitidos; buscar novo CSRF")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginCredentialsRequest request,
                                     HttpServletRequest servletRequest, HttpServletResponse response) {
        var tokens = auth.login(request.email(), request.password(), context(servletRequest));
        rotateCsrf(servletRequest, response);
        return ResponseEntity.noContent().headers(cookies.issue(tokens)).build();
    }

    @PostMapping("/refresh")
    @ApiResponse(responseCode = "204", description = "Rotaciona refresh_token do cookie e CSRF; sem corpo")
    public ResponseEntity<Void> refresh(@CookieValue(name = "refresh_token", required = false) String token,
                                       HttpServletRequest request, HttpServletResponse response) {
        var tokens = auth.refresh(token, context(request));
        rotateCsrf(request, response);
        return ResponseEntity.noContent().headers(cookies.issue(tokens)).build();
    }

    @PostMapping("/logout")
    @ApiResponse(responseCode = "204", description = "Revoga refresh e apaga cookies; idempotente, exige CSRF")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String token,
                                      HttpServletRequest request) {
        if (token != null && !token.isBlank()) auth.logout(token, context(request));
        // Keep the anonymous CSRF session usable for idempotent retries and a later login.
        return ResponseEntity.noContent().headers(cookies.clear()).build();
    }

    private void rotateCsrf(HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) request.changeSessionId();
        csrfTokens.saveToken(null, request, response);
    }

    private AuthRequestContext context(HttpServletRequest request) {
        return new AuthRequestContext(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
