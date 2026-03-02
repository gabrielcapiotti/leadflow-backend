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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;
    private UUID userId;

    private static final String SECRET =
            "test-secret-key-for-jwt-token-generation-minimum-256-bits-required";

    private static final String TENANT = "test_schema";

    @BeforeEach
    void setUp() throws Exception {

        jwtService = new JwtService(
                SECRET,
                3_600_000L,
                "leadflow",
                Clock.systemUTC()
        );

        // 🔥 Importante para versão enterprise
        jwtService.afterPropertiesSet();

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
        assertThat(jwtToken.getToken().split("\\.")).hasSize(3);
        assertThat(jwtToken.getTokenId()).isNotBlank();
        assertThat(jwtToken.getExpiresAt()).isAfter(Instant.now());
    }

    /* ==========================
       VALIDATION
       ========================== */

    @Test
    void isValid_ShouldReturnTrue_ForValidToken() {

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        assertThat(jwtService.isValid(jwtToken.getToken()))
                .isTrue();
    }

    @Test
    void isValid_ShouldReturnFalse_ForInvalidToken() {

        assertThat(jwtService.isValid("invalid.jwt.token"))
                .isFalse();
    }

    /* ==========================
       CLAIM EXTRACTION
       ========================== */

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        assertThat(jwtService.extractEmail(jwtToken.getToken()))
                .isEqualTo("test@example.com");
    }

    @Test
    void extractUserId_ShouldReturnCorrectUserId() {

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        assertThat(jwtService.extractUserId(jwtToken.getToken()))
                .isEqualTo(userId);
    }

    @Test
    void extractRole_ShouldReturnCorrectRole() {

        JwtToken jwtToken = jwtService.generateToken(user, TENANT);

        assertThat(jwtService.extractRole(jwtToken.getToken()))
                .isEqualTo("ROLE_USER");
    }

    @Test
    void extractTenant_ShouldReturnCorrectTenant() {

        JwtToken jwtToken =
                jwtService.generateToken(user, "tenant_x");

        assertThat(jwtService.extractTenant(jwtToken.getToken()))
                .isEqualTo("tenant_x");
    }

    /* ==========================
       NEGATIVE CASES
       ========================== */

    @Test
    void generateToken_ShouldThrow_WhenTenantBlank() {

        assertThatThrownBy(() ->
                jwtService.generateToken(user, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateToken_ShouldThrow_WhenUserNull() {

        assertThatThrownBy(() ->
                jwtService.generateToken(null, TENANT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}