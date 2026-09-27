package com.fcproject.adapters.inbound.controllers;

import com.fcproject.application.core.domain.auth.IssuedTokens;
import com.fcproject.application.core.exceptions.LoginBlockedException;
import com.fcproject.application.ports.inbound.AuthInPort;
import com.fcproject.infrastructure.exceptions.GlobalHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private AuthInPort auth;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new AuthController(auth, new com.fcproject.infrastructure.security.AuthCookies(java.time.Clock.systemUTC()), new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository()))
                .setControllerAdvice(new GlobalHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void rejectsInvalidLoginPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void returnsTheCanonicalTokenContract() throws Exception {
        when(auth.login(org.mockito.ArgumentMatchers.eq("user@example.com"),
                org.mockito.ArgumentMatchers.eq("Valid@123"), any()))
                .thenReturn(new IssuedTokens("access", "refresh", 900L, "Bearer", java.time.Instant.now(), java.time.Instant.now().plusSeconds(2592000)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "Valid@123"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.expiresIn").doesNotExist())
                .andExpect(jsonPath("$.acessToken").doesNotExist());
    }

    @Test
    void returnsRetryAfterWhenLoginIsTemporarilyBlocked() throws Exception {
        when(auth.login(org.mockito.ArgumentMatchers.eq("user@example.com"),
                org.mockito.ArgumentMatchers.eq("invalid-password"), any()))
                .thenThrow(new LoginBlockedException(120));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "invalid-password"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "120"))
                .andExpect(jsonPath("$.code").value("LOGIN_TEMPORARILY_BLOCKED"));
    }
}
