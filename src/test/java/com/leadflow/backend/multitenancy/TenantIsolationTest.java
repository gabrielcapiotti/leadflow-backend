package com.leadflow.backend.multitenancy;

import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;

import com.leadflow.backend.multitenancy.context.TenantContext;

import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.tenant.TenantRepository;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;

import com.leadflow.backend.util.TestTenantFactory;

import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Tag("integration")
@Import(TestTenantFactory.class)
class TenantIsolationTest extends IntegrationTestBase {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestTenantFactory testTenantFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setup() {

        TenantContext.clear();

        tenantRepository.deleteAll();

        tenantA = testTenantFactory.createTenant("tenantA");
        tenantB = testTenantFactory.createTenant("tenantB");

        createSchemaIfNotExists(tenantA.getSchemaName());
        createSchemaIfNotExists(tenantB.getSchemaName());

        ensureRoleExists("ROLE_USER");

        initializeTenant(tenantA.getSchemaName());
        initializeTenant(tenantB.getSchemaName());

        TenantContext.clear();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private void createSchemaIfNotExists(String schema) {
        jdbcTemplate.execute(
                "CREATE SCHEMA IF NOT EXISTS \"" + schema + "\""
        );
    }

    private Role ensureRoleExists(String roleName) {
        return roleRepository.findByNameIgnoreCase(roleName)
                .orElseGet(() ->
                        roleRepository.saveAndFlush(new Role(roleName))
                );
    }

    private void initializeTenant(String schema) {

        TenantContext.setTenant(schema);

        jdbcTemplate.execute(
                "SET search_path TO \"" + schema + "\""
        );

        // força inicialização das entidades
        leadRepository.count();
        userRepository.count();
        roleRepository.count();

        TenantContext.clear();
    }

    @Test
    void shouldIsolateDataBetweenSchemas() {

        TenantContext.setTenant(tenantA.getSchemaName());

        Role role =
                roleRepository.findByNameIgnoreCase("ROLE_USER")
                        .orElseThrow();

        User userA =
                userRepository.saveAndFlush(
                        new User(
                                "User A",
                                "a_" + UUID.randomUUID() + "@mail.com",
                                "pass12345",
                                role
                        )
                );

        Lead leadA =
                new Lead(
                        userA.getId(),
                        "Lead A",
                        "lead_a_" + UUID.randomUUID() + "@mail.com",
                        "111"
                );

        leadRepository.saveAndFlush(leadA);

        assertThat(leadRepository.count()).isEqualTo(1);

        TenantContext.clear();
        TenantContext.setTenant(tenantB.getSchemaName());

        assertThat(leadRepository.count()).isZero();
    }

    @Test
    void shouldNotAccessOtherTenantData() {

        TenantContext.setTenant(tenantA.getSchemaName());

        Role role =
                roleRepository.findByNameIgnoreCase("ROLE_USER")
                        .orElseThrow();

        User userA =
                userRepository.saveAndFlush(
                        new User(
                                "User B",
                                "b_" + UUID.randomUUID() + "@mail.com",
                                "pass12345",
                                role
                        )
                );

        Lead leadA =
                leadRepository.saveAndFlush(
                        new Lead(
                                userA.getId(),
                                "Lead B",
                                "lead_b_" + UUID.randomUUID() + "@mail.com",
                                "111"
                        )
                );

        UUID leadId =
                Objects.requireNonNull(leadA.getId());

        TenantContext.clear();
        TenantContext.setTenant(tenantB.getSchemaName());

        assertThat(
                leadRepository.findById(leadId)
        ).isEmpty();
    }
}