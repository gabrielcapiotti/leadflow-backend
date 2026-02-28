package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.service.audit.SecurityAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SecurityAuditService auditService;

    @Mock
    private LoginAuditService loginAuditService;

    @Mock
    private BruteForceProtectionService bruteForceService; // ✅ NOVO MOCK

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    private Role userRole;

    private static final String SCHEMA =
            "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUp() {

        passwordEncoder = new BCryptPasswordEncoder();

        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                auditService,
                loginAuditService,
                bruteForceService,  // ✅ INJETADO
                5,
                5
        );

        userRole = new Role("ROLE_USER");

        TenantContext.setTenant(SCHEMA);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /* ======================================================
       REGISTER
       ====================================================== */

    @Test
    void shouldRegisterUserSuccessfully() {

        when(userRepository
                .existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
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
        assertThat(passwordEncoder.matches("password123",
                result.getPassword())).isTrue();

        verify(userRepository).save(any(User.class));
        verify(auditService)
                .log(any(), any(), any(), anyBoolean(), any(), any(), any());
    }

    /* ======================================================
       AUTHENTICATE SUCCESS
       ====================================================== */

    @Test
    void shouldAuthenticateSuccessfully() {

        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = new User(
                "Test",
                "test@example.com",
                encodedPassword,
                userRole
        );

        when(bruteForceService.isBlocked(anyString(), anyInt()))
                .thenReturn(false);

        when(userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));

        User result = authService.authenticateUser(
                "test@example.com",
                rawPassword
        );

        assertThat(result).isEqualTo(user);

        verify(bruteForceService).reset(anyString());
        verify(auditService)
                .log(any(), any(), any(), eq(true), any(), any(), any());
    }

    /* ======================================================
       AUTHENTICATE WRONG PASSWORD
       ====================================================== */

    @Test
    void shouldThrowWhenPasswordIsInvalid() {

        String encodedPassword =
                passwordEncoder.encode("correct-password");

        User user = new User(
                "Test",
                "test@example.com",
                encodedPassword,
                userRole
        );

        when(bruteForceService.isBlocked(anyString(), anyInt()))
                .thenReturn(false);

        when(userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.authenticateUser(
                        "test@example.com",
                        "wrong-password"
                )
        )
        .isInstanceOf(IllegalArgumentException.class);

        verify(bruteForceService, atLeastOnce())
                .recordFailure(anyString(), anyInt());

        verify(auditService)
                .log(any(), any(), any(), eq(false), any(), any(), any());
    }

    /* ======================================================
       BRUTE FORCE BLOCK
       ====================================================== */

    @Test
    void shouldBlockWhenBruteForceDetected() {

        when(bruteForceService.isBlocked(anyString(), anyInt()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                authService.authenticateUser(
                        "test@example.com",
                        "password"
                )
        )
        .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(userRepository);
    }
}