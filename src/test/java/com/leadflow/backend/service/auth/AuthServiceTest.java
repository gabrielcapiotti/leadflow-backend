package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private AuthService authService;

    private User user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, new BCryptPasswordEncoder());
        userRole = new Role("USER");
        user = new User("Test User", "test@example.com", "encoded-password", userRole);
    }

    @Test
    void registerUser_ShouldThrowException_WhenRoleNotFound() {
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, 
            () -> authService.registerUser("Test User", "test@example.com", "password123"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, 
            () -> authService.authenticateUser("test@example.com", "password123"));
    }
}
