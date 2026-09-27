package com.fcproject.infrastructure.security;

import com.fcproject.adapters.inbound.controllers.AuthController;
import com.fcproject.adapters.inbound.controllers.UserController;
import com.fcproject.application.core.domain.auth.*;
import com.fcproject.application.core.domain.users.UserDomain;
import com.fcproject.application.core.enums.Gender;
import com.fcproject.application.core.exceptions.InvalidRefreshTokenException;
import com.fcproject.application.core.services.AuthService;
import com.fcproject.application.ports.inbound.AuthInPort;
import com.fcproject.application.ports.inbound.userPorts.*;
import com.fcproject.application.ports.outbound.*;
import com.fcproject.infrastructure.config.SecurityConfig;
import com.fcproject.infrastructure.exceptions.GlobalHandler;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class CookieAuthenticationTest {
    static final String ORIGIN = "https://financial-control-front-khaki.vercel.app";
    static final UUID ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final AuthenticatedUser USER = new AuthenticatedUser(ID, "user@example.com", Set.of("ROLE_USER"));
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    MockHttpSession session;
    String csrf;
    JwtTokenAdapter jwt;

    @BeforeEach void setup() throws Exception {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "testCors", Map.of("security.cors.allowed-origins", ORIGIN)));
        context.register(Config.class);
        context.refresh();
        mvc = webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        jwt = context.getBean(JwtTokenAdapter.class);
        obtainCsrf();
    }
    @AfterEach void close() {
        context.close();
    }
    void obtainCsrf() throws Exception {
        var request = get("/api/v1/auth/csrf");
        if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN")).andReturn();
        session = (MockHttpSession) result.getRequest().getSession(false);
        csrf = new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
    MockHttpServletRequestBuilder mutation(String path) {
        return post(path).session(session).header("X-CSRF-TOKEN", csrf);
    }
    MvcResult login() throws Exception {
        return mvc.perform(mutation("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"Valid@123\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
    }
    @Test void completeFlowRotatesRevokesAndNeverExposesTokensInJson() throws Exception {
        String oldSessionId = session.getId();
        String oldCsrf = csrf;
        var loggedIn = login();
        assertThat(session.getId()).isNotEqualTo(oldSessionId);
        var access = loggedIn.getResponse().getCookie("access_token");
        var refresh = loggedIn.getResponse().getCookie("refresh_token");
        assertCookie(access, "/api/v1", 900);
        assertCookie(refresh, "/api/v1/auth", 30 * 86400);
        assertThat(loggedIn.getResponse().getHeaders("Set-Cookie")).hasSize(2);
        mvc.perform(get("/api/v1/users/me").cookie(access)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER.email()))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/v1/auth/refresh").session(session).cookie(refresh)
                .header("X-CSRF-TOKEN", oldCsrf)).andExpect(status().isForbidden());
        obtainCsrf();
        String preRefreshCsrf = csrf;
        var renewed = mvc.perform(mutation("/api/v1/auth/refresh").cookie(refresh,
                        new Cookie("access_token", "expired-access")))
                .andExpect(status().isNoContent()).andExpect(content().string("")).andReturn();
        var newRefresh = renewed.getResponse().getCookie("refresh_token");
        assertThat(newRefresh.getValue()).isNotEqualTo(refresh.getValue());
        mvc.perform(post("/api/v1/auth/logout").session(session).cookie(newRefresh)
                .header("X-CSRF-TOKEN", preRefreshCsrf)).andExpect(status().isForbidden());
        obtainCsrf();
        mvc.perform(mutation("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
        var logout = mvc.perform(mutation("/api/v1/auth/logout").cookie(newRefresh))
                .andExpect(status().isNoContent()).andExpect(content().string("")).andReturn();
        assertCookie(logout.getResponse().getCookie("access_token"), "/api/v1", 0);
        assertCookie(logout.getResponse().getCookie("refresh_token"), "/api/v1/auth", 0);
        mvc.perform(mutation("/api/v1/auth/refresh").cookie(newRefresh)).andExpect(status().isUnauthorized());
        mvc.perform(mutation("/api/v1/auth/logout").cookie(newRefresh)).andExpect(status().isNoContent());
        mvc.perform(mutation("/api/v1/auth/logout")).andExpect(status().isNoContent());
        mvc.perform(mutation("/api/v1/auth/logout").cookie(new Cookie("refresh_token", "unknown")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void bearerSelectsItsOwnIdentityWhenBothCredentialsAreValidAndDisabledUsersAreRejected() throws Exception {
        UUID otherId = UUID.randomUUID();
        var other = new AuthenticatedUser(otherId, "other@example.com", Set.of("ROLE_USER"));
        var users = context.getBean(DatabaseUserDetailsService.class);
        when(users.loadUserById(otherId)).thenReturn(User.withUsername(other.email()).password("unused").roles("USER").build());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + jwt.generate(USER, Instant.now()))
                .cookie(new Cookie("access_token", jwt.generate(other, Instant.now()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(USER.email()));
        verify(users, never()).loadUserById(otherId);
        when(users.loadUserById(ID)).thenReturn(User.withUsername(USER.email()).password("unused").roles("USER").disabled(true).build());
        mvc.perform(get("/api/v1/users/me").cookie(new Cookie("access_token", jwt.generate(USER, Instant.now()))))
                .andExpect(status().isUnauthorized());
    }
    @Test void expiredCsrfSessionCanBeRecoveredAndJsonRefreshIsNotAccepted() throws Exception {
        session.invalidate();
        session = null;
        mvc.perform(post("/api/v1/auth/logout").header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isForbidden()).andExpect(header().string("Cache-Control", "no-store"));
        obtainCsrf();
        mvc.perform(mutation("/api/v1/auth/logout")).andExpect(status().isNoContent());
        mvc.perform(mutation("/api/v1/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"legacy-json-token\"}"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"));
    }
    void assertCookie(Cookie cookie, String path, int maxAge) {
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("None");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getPath()).isEqualTo(path);
        assertThat(cookie.getMaxAge()).isBetween(Math.max(0, maxAge - 3), maxAge);
    }
    @Test void rejectsMissingInvalidAndExpiredAccessTokensAndHonorsBearerPrecedence() throws Exception {
        String valid = jwt.generate(USER, Instant.now());
        String expired = jwt.generate(USER, Instant.now().minusSeconds(3600));
        for (String token : List.of("bad", expired)) {
            mvc.perform(get("/api/v1/users/me").cookie(new Cookie("access_token", token)))
                    .andExpect(status().isUnauthorized()).andExpect(content().contentType("application/problem+json"));
        }
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + valid)
                .cookie(new Cookie("access_token", "bad"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer bad")
                .cookie(new Cookie("access_token", valid))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Basic bad")
                .cookie(new Cookie("access_token", valid))).andExpect(status().isUnauthorized());
        mvc.perform(mutation("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }
    @Test void rejectsCsrfForEveryMutationIncludingBearerAndRegistration() throws Exception {
        for (String path : List.of("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout", "/api/v1/users")) {
            mvc.perform(post(path).session(session)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
            mvc.perform(post(path).session(session).header("X-CSRF-TOKEN", "wrong"))
                    .andExpect(status().isForbidden());
        }
        String valid = jwt.generate(USER, Instant.now());
        mvc.perform(post("/api/v1/accounts").cookie(new Cookie("access_token", valid)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/accounts").header("Authorization", "Bearer " + valid))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").session(new MockHttpSession()).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isForbidden());
    }
    @Test void corsAllowsOnlyConfiguredOriginAndPreflightNeedsNoCredentials() throws Exception {
        mvc.perform(options("/api/v1/auth/login").header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token,idempotency-key,authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        for (String origin : List.of("https://evil.example", "https://preview.vercel.app", "http://localhost:4200")) {
            mvc.perform(options("/api/v1/auth/login").header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
        mvc.perform(get("/api/v1/auth/csrf").header("Origin", ORIGIN))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
        mvc.perform(get("/api/v1/auth/csrf").header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Configuration @EnableWebMvc @EnableWebSecurity @Import(SecurityConfig.class)
    static class Config {
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean JwtTokenAdapter jwt() { return new JwtTokenAdapter("test", "test-secret-with-at-least-thirty-two-bytes", "", "api", "client", 15); }
        @Bean RestAuthenticationEntryPoint entry() { return new RestAuthenticationEntryPoint(new ObjectMapper()); }
        @Bean DatabaseUserDetailsService users() {
            var users = mock(DatabaseUserDetailsService.class);
            when(users.loadUserById(ID)).thenReturn(User.withUsername(USER.email()).password("unused").roles("USER").build());
            return users;
        }
        @Bean JwtAuthenticationFilter jwtFilter(JwtTokenAdapter jwt, DatabaseUserDetailsService users, RestAuthenticationEntryPoint entry) {
            return new JwtAuthenticationFilter(jwt, users, entry);
        }
        @Bean RateLimitFilter rate(Clock clock) {
            return new RateLimitFilter(new ObjectMapper(), mock(SecurityAuditOutPort.class), clock,
                    new FixedWindowRateLimiter(100), false, Map.of(), new RateLimitPolicy("api", 100, Duration.ofMinutes(1)));
        }
        @Bean AuthInPort auth(JwtTokenAdapter jwt, Clock clock) {
            var authentication = mock(AuthenticationOutPort.class);
            when(authentication.authenticate(USER.email(), "Valid@123")).thenReturn(USER);
            when(authentication.loadById(ID)).thenReturn(USER);
            return new AuthService(authentication, new MemoryRefreshTokens(), jwt, mock(LoginAttemptOutPort.class),
                    mock(SecurityAuditOutPort.class), clock, 30, 5, Duration.ofMinutes(15), Duration.ofMinutes(15));
        }
        @Bean AuthController authController(AuthInPort auth, Clock clock, CsrfTokenRepository csrf) {
            return new AuthController(auth, new AuthCookies(clock), csrf);
        }
        @Bean UserController userController() {
            var lookup = mock(FindUserByEmailInPort.class);
            when(lookup.execute(USER.email())).thenReturn(new UserDomain(ID, "Test", "User", USER.email(),
                    "123", Gender.MALE, LocalDate.of(1990, 1, 1)));
            return new UserController(mock(SaveNewUserInPort.class), lookup);
        }
        @Bean GlobalHandler errors() { return new GlobalHandler(); }
    }
    // Stateful port fake: the flow exercises the real AuthService, including hashing, rotation and revocation.
    static class MemoryRefreshTokens implements RefreshTokenOutPort {
        final Map<String, StoredRefreshToken> values = new HashMap<>();
        public Optional<StoredRefreshToken> findActiveByHash(String hash) {
            return Optional.ofNullable(values.get(hash)).filter(t -> !t.isRevoked());
        }
        public void save(UUID user, String hash, Instant expiry) {
            values.put(hash, new StoredRefreshToken(UUID.randomUUID(), user, hash, expiry, null));
        }
        public void rotate(UUID id, UUID user, String hash, Instant expiry, Instant now) {
            var token = values.values().stream().filter(t -> t.id().equals(id) && !t.isRevoked()).findFirst()
                    .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
            revoke(token.id(), now);
            save(user, hash, expiry);
        }
        public void revoke(UUID id, Instant now) {
            values.replaceAll((hash, t) -> t.id().equals(id)
                    ? new StoredRefreshToken(t.id(), t.userId(), hash, t.expiresAt(), now) : t);
        }
        public void revokeByHash(String hash, Instant now) {
            var t = values.get(hash);
            if (t != null) revoke(t.id(), now);
        }
    }
}
