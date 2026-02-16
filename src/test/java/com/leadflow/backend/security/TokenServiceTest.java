package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private TokenService tokenService;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize TokenService with test configuration
        tokenService = new TokenService(
            "test-secret-key-for-jwt-token-generation-minimum-256-bits-required",
            3600000L  // 1 hour
        );

        Role role = new Role("USER");
        setField(role, "id", 1);

        user = new User("Test User", "test@example.com", "password", role);
        setField(user, "id", 1L);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        String tenant = "test_tenant";
        String token = tokenService.generateToken(user, tenant);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void isValid_ShouldReturnTrue_ForValidToken() {
        String tenant = "test_tenant";
        String token = tokenService.generateToken(user, tenant);

        boolean isValid = tokenService.isValid(token);

        assertTrue(isValid);
    }

    @Test
    void isValid_ShouldReturnFalse_ForInvalidToken() {
        String invalidToken = "invalid.jwt.token";

        boolean isValid = tokenService.isValid(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void getEmail_ShouldReturnCorrectEmail() {
        String tenant = "test_tenant";
        String token = tokenService.generateToken(user, tenant);

        String email = tokenService.getEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    void getUserId_ShouldReturnCorrectUserId() {
        String tenant = "test_tenant";
        String token = tokenService.generateToken(user, tenant);

        Long userId = tokenService.getUserId(token);

        assertEquals(1L, userId);
    }

    @Test
    void getRole_ShouldReturnCorrectRole() {
        String tenant = "test_tenant";
        String token = tokenService.generateToken(user, tenant);

        String role = tokenService.getRole(token);

        assertEquals("USER", role);
    }
}
