package com.fcproject.adapters.outbound.security;

import com.fcproject.application.ports.outbound.PasswordHasherOutPort;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BCryptPasswordHasherAdapter implements PasswordHasherOutPort {
    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasherAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
