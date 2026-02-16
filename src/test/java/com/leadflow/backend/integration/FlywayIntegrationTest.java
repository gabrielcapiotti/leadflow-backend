package com.leadflow.backend.integration;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration-flyway")
@Transactional
class FlywayIntegrationTest {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Should fail when enum contains value not allowed by Flyway CHECK constraint")
    void shouldFailWhenLeadStatusConstraintIsInconsistent() {

        // Arrange
        Role role = roleRepository.save(new Role("USER"));

        User user = userRepository.save(
                new User("Test", "test@email.com", "pass", role)
        );

        Lead lead = new Lead("Lead Test", "lead@email.com", "123456");
        lead.setUser(user);

        // Enum possui QUALIFIED
        // Mas o CHECK constraint do Flyway NÃO permite QUALIFIED
        lead.setStatus(LeadStatus.QUALIFIED);

        // Act + Assert
        assertThatThrownBy(() ->
                leadRepository.saveAndFlush(lead)
        )
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasRootCauseInstanceOf(PSQLException.class)
        .hasMessageContaining("constraint");
    }
}
