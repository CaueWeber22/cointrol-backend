package com.fcproject.application.core.services;

import com.fcproject.application.core.domain.auth.AuthenticatedUser;
import com.fcproject.application.core.domain.auth.AuthRequestContext;
import com.fcproject.application.core.domain.auth.IssuedTokens;
import com.fcproject.application.core.domain.auth.LoginAttemptState;
import com.fcproject.application.core.domain.auth.SecurityEventType;
import com.fcproject.application.core.domain.auth.StoredRefreshToken;
import com.fcproject.application.core.exceptions.InvalidCredentialsException;
import com.fcproject.application.core.exceptions.InvalidRefreshTokenException;
import com.fcproject.application.core.exceptions.LoginBlockedException;
import com.fcproject.application.ports.outbound.AccessTokenOutPort;
import com.fcproject.application.ports.outbound.AuthenticationOutPort;
import com.fcproject.application.ports.outbound.LoginAttemptOutPort;
import com.fcproject.application.ports.outbound.RefreshTokenOutPort;
import com.fcproject.application.ports.outbound.SecurityAuditOutPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-16T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TOKEN_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private AuthenticationOutPort authentication;

    @Mock
    private RefreshTokenOutPort refreshTokens;

    @Mock
    private AccessTokenOutPort accessTokens;

    @Mock
    private LoginAttemptOutPort loginAttempts;

    @Mock
    private SecurityAuditOutPort securityAudit;

    private AuthService service;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                authentication,
                refreshTokens,
                accessTokens,
                loginAttempts,
                securityAudit,
                Clock.fixed(NOW, ZoneOffset.UTC),
                30,
                5,
                Duration.ofMinutes(15),
                Duration.ofMinutes(15)
        );
        user = new AuthenticatedUser(USER_ID, "user@example.com", Set.of("ROLE_USER"));
    }

    @Test
    void logsInAndStoresOnlyTheRefreshTokenHash() {
        when(authentication.authenticate("user@example.com", "Valid@123")).thenReturn(user);
        when(accessTokens.generate(user, NOW)).thenReturn("signed-access-token");
        when(accessTokens.expirationSeconds()).thenReturn(900L);

        IssuedTokens result = service.login(
                " User@Example.com ", "Valid@123", context()
        );

        assertEquals("signed-access-token", result.accessToken());
        assertEquals(128, result.refreshToken().length());
        assertEquals(900L, result.expiresIn());
        verify(refreshTokens).save(eq(USER_ID), any(String.class), eq(Instant.parse("2026-09-15T12:00:00Z")));
        verify(loginAttempts).clear(any(String.class));
        verify(securityAudit).record(org.mockito.ArgumentMatchers.argThat(
                event -> event.eventType() == SecurityEventType.LOGIN_SUCCESS && USER_ID.equals(event.userId())
        ));
    }

    @Test
    void rotatesAnActiveRefreshToken() {
        StoredRefreshToken stored = new StoredRefreshToken(
                TOKEN_ID,
                USER_ID,
                "stored-hash",
                NOW.plusSeconds(60),
                null
        );
        when(refreshTokens.findActiveByHash(any())).thenReturn(Optional.of(stored));
        when(authentication.loadById(USER_ID)).thenReturn(user);
        when(accessTokens.generate(user, NOW)).thenReturn("new-access-token");
        when(accessTokens.expirationSeconds()).thenReturn(900L);

        IssuedTokens result = service.refresh("old-refresh-token", context());

        assertEquals("new-access-token", result.accessToken());
        assertNotEquals("old-refresh-token", result.refreshToken());
        verify(refreshTokens).rotate(
                eq(TOKEN_ID),
                eq(USER_ID),
                any(String.class),
                eq(Instant.parse("2026-09-15T12:00:00Z")),
                eq(NOW)
        );
        verify(securityAudit).record(org.mockito.ArgumentMatchers.argThat(
                event -> event.eventType() == SecurityEventType.TOKEN_REFRESH_SUCCESS
        ));
    }

    @Test
    void revokesAndRejectsAnExpiredRefreshToken() {
        StoredRefreshToken stored = new StoredRefreshToken(
                TOKEN_ID,
                USER_ID,
                "stored-hash",
                NOW,
                null
        );
        when(refreshTokens.findActiveByHash(any())).thenReturn(Optional.of(stored));

        assertThrows(InvalidRefreshTokenException.class, () ->
                service.refresh("expired-refresh-token", context()));

        verify(refreshTokens).revoke(TOKEN_ID, NOW);
        verify(authentication, never()).loadById(any());
        verify(securityAudit).record(org.mockito.ArgumentMatchers.argThat(
                event -> event.eventType() == SecurityEventType.TOKEN_REFRESH_FAILURE
        ));
    }

    @Test
    void revokesRefreshTokenWhenTheUserCanNoLongerAuthenticate() {
        StoredRefreshToken stored = new StoredRefreshToken(
                TOKEN_ID,
                USER_ID,
                "stored-hash",
                NOW.plusSeconds(60),
                null
        );
        when(refreshTokens.findActiveByHash(any())).thenReturn(Optional.of(stored));
        when(authentication.loadById(USER_ID)).thenThrow(new InvalidCredentialsException());

        assertThrows(InvalidRefreshTokenException.class, () ->
                service.refresh("refresh-token", context()));

        verify(refreshTokens).revoke(TOKEN_ID, NOW);
        verify(securityAudit).record(org.mockito.ArgumentMatchers.argThat(
                event -> event.eventType() == SecurityEventType.TOKEN_REFRESH_FAILURE
        ));
    }

    @Test
    void rejectsLoginWhileIdentifierIsTemporarilyLocked() {
        when(loginAttempts.find(any(String.class)))
                .thenReturn(Optional.of(new LoginAttemptState(5, NOW.plusSeconds(120))));

        LoginBlockedException exception = assertThrows(LoginBlockedException.class, () ->
                service.login("user@example.com", "Valid@123", context()));

        assertEquals(120, exception.getRetryAfterSeconds());
        verify(authentication, never()).authenticate(any(), any());
        verify(securityAudit).record(org.mockito.ArgumentMatchers.argThat(
                event -> event.eventType() == SecurityEventType.LOGIN_BLOCKED
        ));
    }

    @Test
    void recordsFailedLoginAndLocksAtConfiguredThreshold() {
        when(authentication.authenticate("user@example.com", "invalid"))
                .thenThrow(new InvalidCredentialsException());
        when(loginAttempts.recordFailure(
                any(String.class), eq(NOW), eq(5), eq(Duration.ofMinutes(15)), eq(Duration.ofMinutes(15))
        )).thenReturn(new LoginAttemptState(5, NOW.plus(Duration.ofMinutes(15))));

        assertThrows(InvalidCredentialsException.class, () ->
                service.login("user@example.com", "invalid", context()));

        verify(securityAudit).record(org.mockito.ArgumentMatchers.argThat(
                event -> event.eventType() == SecurityEventType.LOGIN_BLOCKED
                        && event.userId() == null
                        && event.identifierHash() != null
        ));
    }

    private AuthRequestContext context() {
        return new AuthRequestContext("127.0.0.1", "JUnit");
    }
}
