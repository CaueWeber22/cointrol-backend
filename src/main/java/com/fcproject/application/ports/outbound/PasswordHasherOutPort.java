package com.fcproject.application.ports.outbound;

public interface PasswordHasherOutPort {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
