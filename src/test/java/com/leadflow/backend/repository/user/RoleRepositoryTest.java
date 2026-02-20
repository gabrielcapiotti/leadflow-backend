package com.leadflow.backend.repository.user;

import com.leadflow.backend.entities.user.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();
        roleRepository.saveAndFlush(new Role("ADMIN"));
    }

    /* ======================================================
       FIND BY NAME (CASE INSENSITIVE)
       ====================================================== */

    @Test
    @DisplayName("Should return role when name exists (ignore case)")
    void findByNameIgnoreCase_ShouldReturnRole() {

        Optional<Role> found =
                roleRepository.findByNameIgnoreCase("admin");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ADMIN");
    }

    /* ======================================================
       NOT FOUND
       ====================================================== */

    @Test
    @DisplayName("Should return empty when name does not exist")
    void findByNameIgnoreCase_ShouldReturnEmpty() {

        Optional<Role> found =
                roleRepository.findByNameIgnoreCase("USER");

        assertThat(found).isEmpty();
    }

    /* ======================================================
       CASE INSENSITIVITY VALIDATION
       ====================================================== */

    @Test
    @DisplayName("Should ignore case when searching role")
    void shouldIgnoreCase() {

        Optional<Role> foundLower =
                roleRepository.findByNameIgnoreCase("admin");

        Optional<Role> foundMixed =
                roleRepository.findByNameIgnoreCase("AdMiN");

        assertThat(foundLower).isPresent();
        assertThat(foundMixed).isPresent();
    }
}