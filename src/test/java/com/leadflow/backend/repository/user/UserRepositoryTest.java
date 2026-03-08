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
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.multiTenancy=NONE",
        "multitenancy.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role role;
    private String email;

    @BeforeEach
    void setUp() {

        TenantContext.setTenant("public");

        /*
         Limpa apenas usuários.
         Não remove roles para evitar conflito com seeds ou FKs.
        */
        userRepository.deleteAll();

        /*
         Reutiliza ROLE_USER se já existir (seed do Flyway).
        */
        role = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.saveAndFlush(new Role("ROLE_USER")));

        email = UUID.randomUUID() + "@example.com";

        User user = new User(
                "Test User",
                email,
                "encoded-password",
                role
        );

        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ======================================================
       FIND ACTIVE BY EMAIL
       ====================================================== */

    @Test
    @DisplayName("Should return user when active email exists")
    void findByEmailIgnoreCaseAndDeletedAtIsNull_ShouldReturnUser() {

        Optional<User> found =
                userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email);

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

    /* ======================================================
       EXISTS ACTIVE EMAIL
       ====================================================== */

    @Test
    @DisplayName("Should return true for active user")
    void existsByEmailIgnoreCaseAndDeletedAtIsNull_ShouldReturnTrue() {

        boolean exists =
                userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when user is soft deleted")
    void shouldNotReturnDeletedUser() {

        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow();

        user.softDelete();

        userRepository.saveAndFlush(user);

        boolean exists =
                userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);

        assertThat(exists).isFalse();
    }

    /* ======================================================
       UNIQUE CONSTRAINT
       ====================================================== */

    @Test
    @DisplayName("Should not allow duplicate email")
    void shouldNotAllowDuplicateEmail() {

        User duplicate = new User(
                "Another User",
                email,
                "password",
                role
        );

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}