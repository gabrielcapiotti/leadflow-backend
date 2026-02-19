package com.leadflow.backend.integration;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.repository.tenant.TenantRepository;
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

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @DisplayName("Should persist lead with QUALIFIED status when Flyway constraint is correct")
    void shouldPersistLeadWithQualifiedStatus() {

        // ===== TENANT =====
        Tenant tenant = tenantRepository.save(
                new Tenant("Test Tenant", "test_schema_flyway")
        );

        // ===== ROLE =====
        Role role = roleRepository.save(
                new Role("USER")
        );

        // ===== USER =====
        User user = userRepository.save(
                new User(
                        "Test",
                        "test@email.com",
                        "pass",
                        role,
                        tenant
                )
        );

        // ===== LEAD =====
        Lead lead = new Lead(
                "Lead Test",
                "lead@email.com",
                "123456"
        );

        lead.setUser(user);
        lead.setTenant(tenant);

        lead.changeStatus(LeadStatus.QUALIFIED);

        Lead saved = leadRepository.saveAndFlush(lead);

        assertThat(saved.getStatus())
                .isEqualTo(LeadStatus.QUALIFIED);
    }
}
