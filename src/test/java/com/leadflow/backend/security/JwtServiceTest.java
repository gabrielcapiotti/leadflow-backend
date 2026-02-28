package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.security.jwt.JwtToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
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
                "leadflow",
                Clock.systemUTC()
        );

        userId = UUID.randomUUID();

        Role role = new Role("ROLE_USER");

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

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        assertThat(jwtToken).isNotNull();
        assertThat(jwtToken.getToken()).isNotBlank();

        assertThat(jwtToken.getToken().split("\\."))
                .hasSize(3);

        assertThat(jwtToken.getTokenId()).isNotBlank();
        assertThat(jwtToken.getExpiresAt()).isAfter(Instant.now());
    }

    /* ==========================
       VALIDATION
       ========================== */

    @Test
    void isValid_ShouldReturnTrue_ForValidToken() {

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        boolean isValid = jwtService.isValid(jwtToken.getToken());

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

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        String email = jwtService.extractEmail(jwtToken.getToken());

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void extractUserId_ShouldReturnCorrectUserId() {

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        UUID extractedUserId =
                jwtService.extractUserId(jwtToken.getToken());

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void extractRole_ShouldReturnCorrectRole() {

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        String role =
                jwtService.extractRole(jwtToken.getToken());

        assertThat(role).isEqualTo("ROLE_USER");
    }

    @Test
    void extractTenant_ShouldReturnCorrectTenant() {

        JwtToken jwtToken =
                jwtService.generateToken(user, "tenant_x");

        String tenant =
                jwtService.extractTenant(jwtToken.getToken());

        assertThat(tenant).isEqualTo("tenant_x");
    }
}