package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.repository.tenant.TenantRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.leadflow.backend.multitenancy.context.TenantContext;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TenantRepository tenantRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    private Role userRole;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        authService = new AuthService(
                userRepository,
                roleRepository,
                tenantRepository,
                passwordEncoder
        );

        userRole = new Role("ROLE_USER");
        tenant = new Tenant("Default Tenant", "tenant_test");

        TenantContext.setTenant("tenant_test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /* ==========================
       REGISTER
       ========================== */

    @Test
    void shouldRegisterUserSuccessfully() {

        when(tenantRepository.findBySchemaName("tenant_test"))
                .thenReturn(Optional.of(tenant));

        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(false);

        when(roleRepository.findByNameIgnoreCase("ROLE_USER"))
                .thenReturn(Optional.of(userRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.registerUser(
                "Test User",
                "test@example.com",
                "password123"
        );

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(passwordEncoder.matches("password123", result.getPassword()))
                .isTrue();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {

        when(tenantRepository.findBySchemaName("tenant_test"))
                .thenReturn(Optional.of(tenant));

        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                authService.registerUser("Test", "test@example.com", "password")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRoleNotFound() {

        when(tenantRepository.findBySchemaName("tenant_test"))
                .thenReturn(Optional.of(tenant));

        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(false);

        when(roleRepository.findByNameIgnoreCase("ROLE_USER"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.registerUser("Test", "test@example.com", "password")
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ROLE_USER");
    }

    /* ==========================
       AUTHENTICATE
       ========================== */

    @Test
    void shouldAuthenticateSuccessfully() {

        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = new User(
                "Test",
                "test@example.com",
                encodedPassword,
                userRole,
                tenant
        );

        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));

        User result = authService.authenticateUser(
                "test@example.com",
                rawPassword
        );

        assertThat(result).isEqualTo(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.authenticateUser("test@example.com", "password")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid credentials");
    }

    @Test
    void shouldThrowWhenPasswordIsInvalid() {

        String encodedPassword = passwordEncoder.encode("correct-password");

        User user = new User(
                "Test",
                "test@example.com",
                encodedPassword,
                userRole,
                tenant
        );

        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.authenticateUser("test@example.com", "wrong-password")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid credentials");
    }
}
