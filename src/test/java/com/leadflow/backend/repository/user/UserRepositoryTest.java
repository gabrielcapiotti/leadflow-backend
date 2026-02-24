package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("jpa")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role role;

    @BeforeEach
    void setUp() {

        TenantContext.setTenant("public");

        roleRepository.deleteAll();
        userRepository.deleteAll();

        role = roleRepository.saveAndFlush(
                new Role("ROLE_USER")
        );

        User user = new User(
                "Test User",
                "test@example.com",
                "encoded-password",
                role
        );

        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ==========================
       FIND ACTIVE BY EMAIL
       ========================== */

    @Test
    @DisplayName("Should return user when active email exists")
    void findByEmailIgnoreCaseAndDeletedAtIsNull_ShouldReturnUser() {

        Optional<User> found =
                userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void findByEmailIgnoreCaseAndDeletedAtIsNull_ShouldReturnEmpty() {

        Optional<User> found =
                userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("nonexistent@example.com");

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
                userRepository
                        .findByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com")
                        .orElseThrow();

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
    @DisplayName("Should not allow duplicate email within same schema")
    void shouldNotAllowDuplicateEmail() {

        User duplicate = new User(
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