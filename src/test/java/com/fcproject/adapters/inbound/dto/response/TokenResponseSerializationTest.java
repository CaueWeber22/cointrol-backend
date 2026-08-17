package com.fcproject.adapters.inbound.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcproject.application.core.domain.auth.IssuedTokens;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenResponseSerializationTest {

    @Test
    void serializesOnlyTheCorrectAccessTokenProperty() throws Exception {
        TokenResponse response = TokenResponse.from(
                new IssuedTokens("access", "refresh", 900L, "Bearer")
        );

        String json = new ObjectMapper().writeValueAsString(response);

        assertTrue(json.contains("\"accessToken\":\"access\""));
        assertFalse(json.contains("acessToken"));
        assertFalse(json.contains("authenticated"));
    }
}
