package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceExpirationTest {

    @Test
    void shouldReturnFalseWhenTokenIsExpired() throws InterruptedException {

        // Secret com pelo menos 32 caracteres
        String secret = "test-secret-key-with-at-least-32-characters-long";

        // Expiração extremamente curta (5ms)
        long expirationMillis = 5L;

        TokenService tokenService =
                new TokenService(secret, expirationMillis);

        Role role = new Role("USER");
        User user = new User("Test", "test@example.com", "password", role);

        String token = tokenService.generateToken(user, "test_tenant");

        // Aguarda expirar
        Thread.sleep(15);

        boolean isValid = tokenService.isValid(token);

        assertThat(isValid).isFalse();
    }
}
