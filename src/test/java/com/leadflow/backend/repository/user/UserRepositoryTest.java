package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.tenant.TenantRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Role role;
    private Tenant tenant;

    @BeforeEach
    void setUp() {

        TenantContext.setTenant("public");

        tenant = tenantRepository.saveAndFlush(
                new Tenant(
                        "Test Tenant " + UUID.randomUUID(),
                        "test_schema_" + UUID.randomUUID().toString().replace("-", "")
                )
        );

        role = roleRepository.saveAndFlush(
                new Role("ROLE_USER")
        );

        User user = new User(
                "Test User",
                "test@example.com",
                "encoded-password",
                role,
                tenant
        );

        userRepository.saveAndFlush(user);
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
    @DisplayName("Should not allow duplicate email within same tenant")
    void shouldNotAllowDuplicateEmail() {

        User duplicate = new User(
                "Another User",
                "test@example.com",
                "password",
                role,
                tenant
        );

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(duplicate)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
