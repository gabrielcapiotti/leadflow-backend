package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceExpirationTest {

    @Test
    void shouldReturnFalseWhenTokenIsExpired() throws InterruptedException {

        // Secret válido (>= 32 chars)
        String secret = "test-secret-key-with-at-least-32-characters-long";

        // Expiração curta mas não absurda
        long expirationMillis = 50L;

        TokenService tokenService =
                new TokenService(secret, expirationMillis);

        Role role = new Role("USER");
        User user = new User("Test", "test@example.com", "password", role);

        String token = tokenService.generateToken(user, "test_tenant");

        // Espera suficiente para garantir expiração
        Thread.sleep(100);

        boolean isValid = tokenService.isValid(token);

        assertThat(isValid).isFalse();
    }
}
