package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role role;

    @BeforeEach
    void setUp() {

        role = roleRepository.saveAndFlush(
                new Role("ROLE_USER")
        );

        userRepository.saveAndFlush(
                new User(
                        "Test User",
                        "test@example.com",
                        "encoded-password",
                        role
                )
        );
    }

    /* ==========================
       FIND BY EMAIL
       ========================== */

    @Test
    @DisplayName("Should return user when email exists")
    void findByEmail_ShouldReturnUser() {

        Optional<User> found =
                userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void findByEmail_ShouldReturnEmpty() {

        Optional<User> found =
                userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    /* ==========================
       EXISTS ACTIVE EMAIL
       ========================== */

    @Test
    @DisplayName("Should return true for active user")
    void existsByEmailIgnoreCaseAndDeletedAtIsNull_ShouldReturnTrue() {

        boolean exists =
                userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when user is soft deleted")
    void shouldNotReturnDeletedUser() {

        User user =
                userRepository.findByEmail("test@example.com").orElseThrow();

        user.softDelete();
        userRepository.saveAndFlush(user);

        boolean exists =
                userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com");

        assertThat(exists).isFalse();
    }

    /* ==========================
       UNIQUE CONSTRAINT
       ========================== */

    @Test
    @DisplayName("Should not allow duplicate email")
    void shouldNotAllowDuplicateEmail() {

        User duplicate =
                new User(
                        "Another User",
                        "test@example.com",
                        "password",
                        role
                );

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(duplicate)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
