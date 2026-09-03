package com.fcproject.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcproject.application.ports.outbound.SecurityAuditOutPort;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {
    private static final Instant NOW = Instant.parse("2026-08-23T12:00:10Z");

    @Test
    void returnsProblemDetailsAndRetryAfterWhenLimitIsExceeded() throws Exception {
        SecurityAuditOutPort audit = mock(SecurityAuditOutPort.class);
        RateLimitFilter filter = filter(audit);

        MockHttpServletRequest firstRequest = loginRequest();
        filter.doFilter(firstRequest, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletResponse rejectedResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), rejectedResponse, new MockFilterChain());

        assertEquals(429, rejectedResponse.getStatus());
        assertEquals("50", rejectedResponse.getHeader("Retry-After"));
        assertTrue(rejectedResponse.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
        verify(audit).record(any());
    }

    private RateLimitFilter filter(SecurityAuditOutPort audit) {
        return new RateLimitFilter(
                new ObjectMapper(),
                audit,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new FixedWindowRateLimiter(100),
                true,
                Map.of(
                        "POST /api/v1/auth/login",
                        new RateLimitPolicy("login", 1, Duration.ofSeconds(60))
                ),
                new RateLimitPolicy("api", 100, Duration.ofSeconds(60))
        );
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        return request;
    }
}
