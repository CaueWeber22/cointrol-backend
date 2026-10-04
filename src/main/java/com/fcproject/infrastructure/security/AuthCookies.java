package com.fcproject.infrastructure.security;

import com.fcproject.application.core.domain.auth.IssuedTokens;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Instant;

@Component
public class AuthCookies {
    private final Clock clock;
    private final boolean secure;
    private final String sameSite;

    public AuthCookies(Clock clock) {
        this(clock, true, "None");
    }

    @Autowired
    public AuthCookies(
            Clock clock,
            @Value("${security.cookies.secure:true}") boolean secure,
            @Value("${security.cookies.same-site:None}") String sameSite
    ) {
        this.clock = clock;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public HttpHeaders issue(IssuedTokens tokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie("access_token", tokens.accessToken(), "/api/v1",
                remaining(tokens.issuedAt().plusSeconds(tokens.expiresIn()))));
        headers.add(HttpHeaders.SET_COOKIE, cookie("refresh_token", tokens.refreshToken(), "/api/v1/auth",
                remaining(tokens.refreshExpiresAt())));
        return headers;
    }
    public HttpHeaders clear() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie("access_token", "", "/api/v1", 0));
        headers.add(HttpHeaders.SET_COOKIE, cookie("refresh_token", "", "/api/v1/auth", 0));
        return headers;
    }
    private long remaining(Instant expiresAt) {
        return Math.max(0, expiresAt.getEpochSecond() - clock.instant().getEpochSecond());
    }
    private String cookie(String name, String value, String path, long seconds) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secure).sameSite(sameSite)
                .path(path).maxAge(seconds).build().toString();
    }
}
