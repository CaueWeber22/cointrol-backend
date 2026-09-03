package com.fcproject.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcproject.application.core.domain.auth.SecurityAuditEvent;
import com.fcproject.application.core.domain.auth.SecurityEventType;
import com.fcproject.application.ports.outbound.SecurityAuditOutPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ObjectMapper objectMapper;
    private final SecurityAuditOutPort securityAudit;
    private final Clock clock;
    private final FixedWindowRateLimiter limiter;
    private final boolean enabled;
    private final Map<String, RateLimitPolicy> exactPolicies;
    private final RateLimitPolicy apiPolicy;

    public RateLimitFilter(
            ObjectMapper objectMapper,
            SecurityAuditOutPort securityAudit,
            Clock clock,
            FixedWindowRateLimiter limiter,
            boolean enabled,
            Map<String, RateLimitPolicy> exactPolicies,
            RateLimitPolicy apiPolicy
    ) {
        this.objectMapper = objectMapper;
        this.securityAudit = securityAudit;
        this.clock = clock;
        this.limiter = limiter;
        this.enabled = enabled;
        this.exactPolicies = Map.copyOf(exactPolicies);
        this.apiPolicy = apiPolicy;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitPolicy policy = policyFor(request);
        if (!enabled || policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIp(request);
        Instant now = clock.instant();
        var decision = limiter.acquire(policy.name() + ':' + clientIp, policy, now);
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (decision.firstRejection()) {
            recordRejection(request, clientIp, now);
        }
        writeTooManyRequests(request, response, decision.retryAfterSeconds());
    }

    private RateLimitPolicy policyFor(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        RateLimitPolicy exact = exactPolicies.get(request.getMethod() + ' ' + path);
        if (exact != null) {
            return exact;
        }
        return path.startsWith("/api/v1/") ? apiPolicy : null;
    }

    private String clientIp(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        if (address == null || address.isBlank()) {
            return "unknown";
        }
        String normalized = address.strip();
        return normalized.length() <= 45 ? normalized : normalized.substring(0, 45);
    }

    private void recordRejection(HttpServletRequest request, String clientIp, Instant occurredAt) {
        try {
            securityAudit.record(new SecurityAuditEvent(
                    UUID.randomUUID(),
                    null,
                    sha256(clientIp),
                    SecurityEventType.RATE_LIMIT_EXCEEDED,
                    clientIp,
                    limited(request.getHeader(HttpHeaders.USER_AGENT), 255),
                    occurredAt
            ));
        } catch (RuntimeException exception) {
            LOGGER.error("Could not persist rate-limit audit event", exception);
        }
    }

    private void writeTooManyRequests(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Request limit exceeded; retry later"
        );
        problem.setTitle("Too many requests");
        problem.setType(URI.create("https://cointrol.dev/problems/rate-limit-exceeded"));
        problem.setProperty("code", "RATE_LIMIT_EXCEEDED");
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("retryAfterSeconds", retryAfterSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private String limited(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        return normalized.length() <= maximumLength ? normalized : normalized.substring(0, maximumLength);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
