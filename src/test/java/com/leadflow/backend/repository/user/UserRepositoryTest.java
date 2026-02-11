package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role("USER");
        entityManager.persist(role);

        user = new User("Test User", "test@example.com", "encoded-password", role);
        entityManager.persist(user);
        entityManager.flush();
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenUserExists() {
        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void existsByEmailAndDeletedAtIsNull_ShouldReturnTrue_WhenEmailExists() {
        boolean exists = userRepository.existsByEmailAndDeletedAtIsNull("test@example.com");

        assertTrue(exists);
    }

    @Test
    void existsByEmailAndDeletedAtIsNull_ShouldReturnFalse_WhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmailAndDeletedAtIsNull("nonexistent@example.com");

        assertFalse(exists);
    }
}
