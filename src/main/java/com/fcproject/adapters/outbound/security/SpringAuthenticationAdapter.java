package com.fcproject.adapters.outbound.security;

import com.fcproject.adapters.outbound.entities.users.UserEntity;
import com.fcproject.adapters.outbound.persistence.UserJPARepository;
import com.fcproject.application.core.domain.auth.AuthenticatedUser;
import com.fcproject.application.ports.outbound.AuthenticationOutPort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpringAuthenticationAdapter implements AuthenticationOutPort {
    private final AuthenticationManager authenticationManager;
    private final UserJPARepository userRepository;

    public SpringAuthenticationAdapter(
            AuthenticationManager authenticationManager,
            UserJPARepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @Override
    public AuthenticatedUser authenticate(String email, String rawPassword) {
        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );
        return toDomain((UserEntity) result.getPrincipal());
    }

    @Override
    public AuthenticatedUser loadById(UUID userId) {
        return userRepository.findWithRolesById(userId)
                .map(this::toDomain)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private AuthenticatedUser toDomain(UserEntity user) {
        Set<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedUser(user.getId(), user.getEmail(), roles);
    }
}
