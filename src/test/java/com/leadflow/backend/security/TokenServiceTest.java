package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private TokenService tokenService;
    private User user;

    private static final String SECRET =
            "test-secret-key-for-jwt-token-generation-minimum-256-bits-required";

    @BeforeEach
    void setUp() throws Exception {

        tokenService = new TokenService(
                SECRET,
                3600000L // 1 hora
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

    /* ==========================
       GENERATE TOKEN
       ========================== */

    @Test
    void generateToken_ShouldReturnValidJwtStructure() {

        String token = tokenService.generateToken(user, "test_tenant");

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

        String token = tokenService.generateToken(user, "test_tenant");

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
                new TokenService(SECRET, 50L);

        String token = shortLivedService.generateToken(user, "test_tenant");

        Thread.sleep(100);

        boolean isValid = shortLivedService.isValid(token);

        assertThat(isValid).isFalse();
    }

    /* ==========================
       CLAIM EXTRACTION
       ========================== */

    @Test
    void getEmail_ShouldReturnCorrectEmail() {

        String token = tokenService.generateToken(user, "test_tenant");

        String email = tokenService.getEmail(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void getUserId_ShouldReturnCorrectUserId() {

        String token = tokenService.generateToken(user, "test_tenant");

        Long userId = tokenService.getUserId(token);

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void getRole_ShouldReturnCorrectRole() {

        String token = tokenService.generateToken(user, "test_tenant");

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
