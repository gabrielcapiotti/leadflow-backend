package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, roleRepository, passwordEncoder);

        userRole = new Role("USER");
    }

    /* ==========================
       REGISTER
       ========================== */

    @Test
    void shouldRegisterUserSuccessfully() {

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

        verify(userRepository).existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com");
        verify(roleRepository).findByNameIgnoreCase("ROLE_USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {

        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                authService.registerUser("Test", "test@example.com", "password")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Email");

        verify(userRepository).existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com");
        verify(roleRepository, never()).findByNameIgnoreCase(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRoleNotFound() {

        when(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(false);

        when(roleRepository.findByNameIgnoreCase("ROLE_USER"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.registerUser("Test", "test@example.com", "password")
        )
        .isInstanceOf(IllegalStateException.class);

        verify(roleRepository).findByNameIgnoreCase("ROLE_USER");
        verify(userRepository, never()).save(any());
    }

    /* ==========================
       AUTHENTICATE
       ========================== */

    @Test
    void shouldAuthenticateSuccessfully() {

        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = new User("Test", "test@example.com", encodedPassword, userRole);

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
        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenPasswordIsInvalid() {

        String encodedPassword = passwordEncoder.encode("correct-password");

        User user = new User("Test", "test@example.com", encodedPassword, userRole);

        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                authService.authenticateUser("test@example.com", "wrong-password")
        )
        .isInstanceOf(IllegalArgumentException.class);
    }
}
