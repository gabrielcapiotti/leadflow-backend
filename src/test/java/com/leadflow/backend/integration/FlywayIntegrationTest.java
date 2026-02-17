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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("Should persist lead with QUALIFIED status when Flyway constraint is correct")
    void shouldPersistLeadWithQualifiedStatus() {

        Role role = roleRepository.save(new Role("USER"));

        User user = userRepository.save(
                new User("Test", "test@email.com", "pass", role)
        );

        Lead lead = new Lead("Lead Test", "lead@email.com", "123456");
        lead.setUser(user);

        // Usa método correto
        lead.changeStatus(LeadStatus.QUALIFIED);

        Lead saved = leadRepository.saveAndFlush(lead);

        assertThat(saved.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
    }
}
