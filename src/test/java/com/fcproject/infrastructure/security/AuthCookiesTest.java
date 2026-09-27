package com.fcproject.infrastructure.security;

import com.fcproject.application.core.domain.auth.IssuedTokens;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class AuthCookiesTest {
    @Test void maxAgeUsesActualExpiryAndNeverExtendsTokensAfterProcessingDelay() {
        Instant issued = Instant.parse("2026-09-27T12:00:00Z");
        var cookies = new AuthCookies(Clock.fixed(issued.plusSeconds(12), ZoneOffset.UTC));
        var headers = cookies.issue(new IssuedTokens("access", "refresh", 300, "Bearer",
                issued, issued.plus(Duration.ofDays(7))));
        assertThat(headers.get("Set-Cookie")).hasSize(2);
        assertThat(headers.get("Set-Cookie").getFirst()).contains("Max-Age=288").doesNotContain("Domain=");
        assertThat(headers.get("Set-Cookie").getLast()).contains("Max-Age=604788").doesNotContain("Domain=");
        var expired = cookies.issue(new IssuedTokens("access", "refresh", 1, "Bearer", issued, issued));
        assertThat(expired.get("Set-Cookie")).allSatisfy(value -> assertThat(value).contains("Max-Age=0"));
    }
}
