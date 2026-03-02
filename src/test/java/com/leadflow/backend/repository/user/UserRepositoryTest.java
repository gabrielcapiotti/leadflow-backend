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

        // Definir o tenant antes de realizar as operações
        TenantContext.setTenant("public");

        // Limpar dados antes de cada teste
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Criar um novo papel e salvar
        role = roleRepository.saveAndFlush(new Role("ROLE_USER"));

        // Definir um e-mail fixo e determinístico para evitar duplicações inesperadas
        email = UUID.randomUUID() + "@example.com";

        // Criar um novo usuário e salvar
        User user = new User(
                "Test User",
                email,
                "encoded-password",  // A senha deve ser codificada, dependendo da implementação
                role
        );
        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void cleanup() {
        // Limpar o contexto do tenant após cada teste
        TenantContext.clear();
    }

    /* ======================================================
       FIND ACTIVE BY EMAIL
       ====================================================== */

    @Test
    @DisplayName("Should return user when active email exists")
    void findByEmailIgnoreCaseAndDeletedAtIsNull_ShouldReturnUser() {
        Optional<User> found = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void findByEmailIgnoreCaseAndDeletedAtIsNull_ShouldReturnEmpty() {
        Optional<User> found = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    /* ======================================================
       EXISTS ACTIVE EMAIL
       ====================================================== */

    @Test
    @DisplayName("Should return true for active user")
    void existsByEmailIgnoreCaseAndDeletedAtIsNull_ShouldReturnTrue() {
        boolean exists = userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when user is soft deleted")
    void shouldNotReturnDeletedUser() {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow();
        
        user.softDelete(); // Certifique-se de que o método softDelete está implementado corretamente.
        userRepository.saveAndFlush(user);

        boolean exists = userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);
        assertThat(exists).isFalse();
    }

    /* ======================================================
       UNIQUE CONSTRAINT
       ====================================================== */

    @Test
    @DisplayName("Should not allow duplicate email")
    void shouldNotAllowDuplicateEmail() {
        // Tentar salvar um usuário com o mesmo email, que já existe no banco de dados
        User duplicate = new User(
                "Another User",
                email,  // Mesmo e-mail do usuário já existente
                "password",
                role
        );

        // Esperamos que a exceção DataIntegrityViolationException seja lançada
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .withFailMessage("Duplicate email should cause a DataIntegrityViolationException");
    }
}