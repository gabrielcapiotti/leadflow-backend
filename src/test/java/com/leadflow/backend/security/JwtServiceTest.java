package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.security.jwt.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;
    private UUID userId;

    private static final String SECRET =
            "test-secret-key-for-jwt-token-generation-minimum-256-bits-required";

    private static final String TENANT = "test_schema";

    @BeforeEach
    void setUp() {

        jwtService = new JwtService(
                SECRET,
                3_600_000L,
                "leadflow"
        );

        userId = UUID.randomUUID();

        // ===== ROLE =====
        Role role = new Role("ROLE_USER");

        // ===== USER =====
        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role
        );

        ReflectionTestUtils.setField(user, "id", userId);
    }

    /* ==========================
       GENERATE TOKEN
       ========================== */

    @Test
    void generateToken_ShouldReturnValidJwtStructure() {

        String token = jwtService.generateToken(user, TENANT);

        assertThat(token)
                .isNotNull()
                .isNotBlank();

        // JWT possui 3 partes separadas por ponto
        assertThat(token.split("\\."))
                .hasSize(3);
    }

    /* ==========================
       VALIDATION
       ========================== */

    @Test
    void isValid_ShouldReturnTrue_ForValidToken() {

        String token = jwtService.generateToken(user, TENANT);

        boolean isValid = jwtService.isValid(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void isValid_ShouldReturnFalse_ForInvalidToken() {

        boolean isValid = jwtService.isValid("invalid.jwt.token");

        assertThat(isValid).isFalse();
    }

    /* ==========================
       CLAIM EXTRACTION
       ========================== */

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {

        String token = jwtService.generateToken(user, TENANT);

        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void extractUserId_ShouldReturnCorrectUserId() {

        String token = jwtService.generateToken(user, TENANT);

        UUID extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void extractRole_ShouldReturnCorrectRole() {

        String token = jwtService.generateToken(user, TENANT);

        String role = jwtService.extractRole(token);

        assertThat(role).isEqualTo("ROLE_USER");
    }

    @Test
    void extractTenant_ShouldReturnCorrectTenant() {

        String token = jwtService.generateToken(user, "tenant_x");

        String tenant = jwtService.extractTenant(token);

        assertThat(tenant).isEqualTo("tenant_x");
    }
}