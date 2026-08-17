package com.fcproject.adapters.outbound.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptPasswordHasherAdapterTest {

    @Test
    void hashesAndMatchesWithoutPersistingTheRawPassword() {
        BCryptPasswordHasherAdapter adapter = new BCryptPasswordHasherAdapter(new BCryptPasswordEncoder(4));

        String hash = adapter.hash("Valid@123");

        assertNotEquals("Valid@123", hash);
        assertTrue(adapter.matches("Valid@123", hash));
    }
}
