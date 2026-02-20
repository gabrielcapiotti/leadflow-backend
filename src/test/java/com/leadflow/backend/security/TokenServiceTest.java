package com.leadflow.backend.security;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private TokenService tokenService;
    private User user;
    private UUID userId;
    private UUID roleId;

    private static final String SECRET =
            "test-secret-key-for-jwt-token-generation-minimum-256-bits-required";

    private static final String TENANT = "test_schema";

    @BeforeEach
    void setUp() {

        tokenService = new TokenService(
                SECRET,
                3_600_000L // 1 hora
        );

        roleId = UUID.randomUUID();
        userId = UUID.randomUUID();

        // ===== TENANT =====
        Tenant tenant = new Tenant(
                "Test Tenant",
                TENANT
        );

        // ===== ROLE =====
        Role role = new Role("USER");
        ReflectionTestUtils.setField(role, "id", roleId);

        // ===== USER =====
        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role,
                tenant
        );

        ReflectionTestUtils.setField(user, "id", userId);
    }

    /* ==========================
       GENERATE TOKEN
       ========================== */

    @Test
    void generateToken_ShouldReturnValidJwtStructure() {

        String token = tokenService.generateToken(user, TENANT);

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

        String token = tokenService.generateToken(user, TENANT);

        boolean isValid = tokenService.isValid(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void isValid_ShouldReturnFalse_ForInvalidToken() {

        boolean isValid = tokenService.isValid("invalid.jwt.token");

        assertThat(isValid).isFalse();
    }

    @Test
    void isValid_ShouldReturnFalse_WhenTokenExpired() throws InterruptedException {

        TokenService shortLivedService =
                new TokenService(SECRET, 1L);

        String token = shortLivedService.generateToken(user, TENANT);

        Thread.sleep(5); // tempo mínimo necessário

        boolean isValid = shortLivedService.isValid(token);

        assertThat(isValid).isFalse();
    }

    /* ==========================
       CLAIM EXTRACTION
       ========================== */

    @Test
    void getEmail_ShouldReturnCorrectEmail() {

        String token = tokenService.generateToken(user, TENANT);

        String email = tokenService.getEmail(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void getUserId_ShouldReturnCorrectUserId() {

        String token = tokenService.generateToken(user, TENANT);

        UUID extractedUserId = tokenService.getUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void getRole_ShouldReturnCorrectRole() {

        String token = tokenService.generateToken(user, TENANT);

        String role = tokenService.getRole(token);

        assertThat(role).isEqualTo("USER");
    }

    @Test
    void getTenant_ShouldReturnCorrectTenant() {

        String token = tokenService.generateToken(user, "tenant_x");

        String tenant = tokenService.getTenant(token);

        assertThat(tenant).isEqualTo("tenant_x");
    }
}