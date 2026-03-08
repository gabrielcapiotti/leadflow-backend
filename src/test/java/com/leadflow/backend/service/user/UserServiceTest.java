package com.leadflow.backend.service.user;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private UUID roleId;

    private User user;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        Role role = new Role("ROLE_USER");
        ReflectionTestUtils.setField(role, "id", roleId);

        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role
        );

        ReflectionTestUtils.setField(user, "id", userId);
    }

    /* ======================================================
       FIND BY EMAIL
       ====================================================== */

    @Test
    void shouldReturnUserWhenEmailExists() {

        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));

        User result = userService.getActiveByEmail("test@example.com");

        assertThat(result).isEqualTo(user);
        verify(userRepository)
                .findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com");
    }

    @Test
    void shouldThrowWhenEmailNotFound() {

        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("notfound@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.getActiveByEmail("notfound@example.com")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    /* ======================================================
       FIND BY ID
       ====================================================== */

    @Test
    void shouldReturnUserById() {

        when(userRepository.findById(Objects.requireNonNull(userId)))
                .thenReturn(Optional.of(user));

        User result = userService.getById(Objects.requireNonNull(userId));

        assertThat(result).isEqualTo(user);
        verify(userRepository).findById(Objects.requireNonNull(userId));
    }

    @Test
    void shouldThrowWhenUserNotFoundById() {

        when(userRepository.findById(Objects.requireNonNull(userId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.getById(Objects.requireNonNull(userId))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}