package com.fcproject.infrastructure.config;

import com.fcproject.infrastructure.security.JwtAuthenticationFilter;
import com.fcproject.infrastructure.security.RateLimitFilter;
import com.fcproject.infrastructure.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class SecurityConfigTest {
    @Test
    void rejectsCorsWildcards() {
        var config = new SecurityConfig(mock(JwtAuthenticationFilter.class), mock(RateLimitFilter.class),
                mock(RestAuthenticationEntryPoint.class));
        assertThatThrownBy(() -> config.corsConfigurationSource("*"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource("https://*.vercel.app"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void configuresAuthenticationFromUserDetailsServiceWithoutWarning(CapturedOutput output) {
        UserDetailsService users = mock(UserDetailsService.class);
        new WebApplicationContextRunner()
                .withUserConfiguration(WebSecurity.class, SecurityConfig.class)
                .withBean(UserDetailsService.class, () -> users)
                .withBean(JwtAuthenticationFilter.class, () -> mock(JwtAuthenticationFilter.class))
                .withBean(RateLimitFilter.class, () -> mock(RateLimitFilter.class))
                .withBean(RestAuthenticationEntryPoint.class, () -> mock(RestAuthenticationEntryPoint.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
                    String hash = encoder.encode("test-password");
                    assertThat(hash).startsWith("$2a$12$");
                    var user = User.withUsername("user@example.com")
                            .password(hash).roles("USER").build();
                    when(users.loadUserByUsername(user.getUsername())).thenReturn(user);
                    when(users.loadUserByUsername("missing@example.com"))
                            .thenThrow(new UsernameNotFoundException("User not found"));

                    AuthenticationManager manager = context.getBean(AuthenticationManager.class);
                    var result = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                            user.getUsername(), "test-password"));
                    assertThat(result.isAuthenticated()).isTrue();
                    assertThat(result.getName()).isEqualTo(user.getUsername());
                    assertThat(result.getCredentials()).isNull();
                    assertThatThrownBy(() -> manager.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated(user.getUsername(), "wrong")))
                            .isInstanceOf(BadCredentialsException.class);
                    assertThatThrownBy(() -> manager.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated("missing@example.com", "wrong")))
                            .isInstanceOf(BadCredentialsException.class);
                });
        assertThat(output.getAll()).doesNotContain("UserDetailsService beans will not be used");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class WebSecurity {
    }
}
