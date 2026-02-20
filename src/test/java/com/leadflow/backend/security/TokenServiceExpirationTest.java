package com.leadflow.backend.security;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

class TokenServiceExpirationTest {

    @Test
    void shouldReturnFalseWhenTokenIsExpired() throws InterruptedException {

        // Secret válido (>= 32 chars)
        String secret = "test-secret-key-with-at-least-32-characters-long";

        // Expiração extremamente curta
        long expirationMillis = 1L;

        TokenService tokenService =
                new TokenService(secret, expirationMillis);

        // ===== TENANT =====
        Tenant tenant = new Tenant(
                "Test Tenant",
                "test_schema"
        );

        // ===== ROLE =====
        Role role = new Role("USER");

        // ===== USER =====
        User user = new User(
                "Test",
                "test@example.com",
                "password",
                role,
                tenant
        );
        user.setId(UUID.randomUUID());

        String token =
                tokenService.generateToken(user, tenant.getSchemaName());

        // Aguarda expiração garantida
        Thread.sleep(5);

        boolean isValid = tokenService.isValid(token);

        assertThat(isValid)
                .as("Token expirado deve ser inválido")
                .isFalse();
    }
}