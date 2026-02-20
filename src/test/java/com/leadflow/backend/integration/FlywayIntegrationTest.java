package com.leadflow.backend.integration;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.repository.tenant.TenantRepository;

import org.flywaydb.core.Flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant tenant;

    @BeforeEach
    void setup() {

        tenant = tenantRepository.save(
                new Tenant("Test Tenant", "test_schema")
        );

        // Ativa o tenant para o MultiTenantConnectionProvider
        TenantContext.setTenant("test_schema");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should persist lead with QUALIFIED status when Flyway constraint is correct")
    void shouldPersistLeadWithQualifiedStatus() {

        Role role = roleRepository.save(
                new Role("USER")
        );

        User user = userRepository.save(
                new User(
                        "Test",
                        "test@email.com",
                        "pass",
                        role,
                        tenant
                )
        );

        // NOVO CONSTRUTOR COMPATÍVEL
        Lead lead = new Lead(
                user.getId(),
                "Lead Test",
                "lead@email.com",
                "123456"
        );

        lead.changeStatus(LeadStatus.QUALIFIED);

        Lead saved = leadRepository.saveAndFlush(lead);

        assertThat(saved.getStatus())
                .isEqualTo(LeadStatus.QUALIFIED);
    }

    @Test
    @DisplayName("Should apply Flyway migrations to tenant schema")
    void shouldApplyFlywayMigrationsToTenantSchema() {

        String tenantSchema = "test_schema";

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + tenantSchema);

        Flyway tenantFlyway = Flyway.configure()
                .dataSource(flyway.getConfiguration().getDataSource())
                .locations("classpath:db/migration")
                .schemas(tenantSchema)
                .baselineOnMigrate(true)
                .load();

        tenantFlyway.migrate();

        Boolean migrationsApplied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) > 0 FROM information_schema.tables WHERE table_schema = ?",
                Boolean.class,
                tenantSchema
        );

        assertTrue(
                Boolean.TRUE.equals(migrationsApplied),
                "Flyway migrations should be applied to the tenant schema"
        );
    }
}