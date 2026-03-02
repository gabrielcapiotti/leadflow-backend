package com.leadflow.backend.multitenancy;

import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.util.TestTenantFactory;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test") // Use o profile 'test' para padronizar os testes
@Tag("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TenantIsolationTest extends IntegrationTestBase {

    @Autowired
    private LeadRepository leadRepository;

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

        tenantA = testTenantFactory.createTenant("Tenant A");
        tenantB = testTenantFactory.createTenant("Tenant B");

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + tenantA.getSchemaName());
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + tenantB.getSchemaName());

        // Initialize tables and ROLE_USER in Tenant A
        TenantContext.setTenant(tenantA.getSchemaName());
        roleRepository.saveAndFlush(new Role("ROLE_USER"));
        leadRepository.count();

        // Initialize tables and ROLE_USER in Tenant B
        TenantContext.setTenant(tenantB.getSchemaName());
        roleRepository.saveAndFlush(new Role("ROLE_USER"));
        leadRepository.count();

        TenantContext.clear();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ======================================================
       TEST 1 — Count Isolation
       ====================================================== */

    @Test
    void shouldIsolateDataBetweenSchemas() {

        // ---------- TENANT A ----------
        TenantContext.setTenant(tenantA.getSchemaName());

        Role role = roleRepository.findByNameIgnoreCase("ROLE_USER")
                .orElseThrow();

        User userA = userRepository.saveAndFlush(
                new User(
                        "User A",
                        "a_" + UUID.randomUUID() + "@mail.com",
                        "pass12345",
                        role
                )
        );

        Lead leadA = new Lead(
                userA.getId(),
                "Lead A",
                "lead_a_" + UUID.randomUUID() + "@mail.com",
                "111"
        );

        leadRepository.saveAndFlush(leadA);

        long countTenantA = leadRepository.count();

        assertThat(countTenantA)
                .as("Tenant A must contain exactly 1 lead")
                .isEqualTo(1);

        // ---------- TENANT B ----------
        TenantContext.clear();
        TenantContext.setTenant(tenantB.getSchemaName());

        long countTenantB = leadRepository.count();

        assertThat(countTenantB)
                .as("Tenant B must not see Tenant A data")
                .isZero();
    }

    /* ======================================================
       TEST 2 — Direct ID Isolation
       ====================================================== */

    @Test
    void shouldNotAccessOtherTenantData() {

        // ---------- TENANT A ----------
        TenantContext.setTenant(tenantA.getSchemaName());

        Role role = roleRepository.findByNameIgnoreCase("ROLE_USER")
                .orElseThrow();

        User userA = userRepository.saveAndFlush(
                new User(
                        "User B",
                        "b_" + UUID.randomUUID() + "@mail.com",
                        "pass12345",
                        role
                )
        );

        Lead leadA = leadRepository.saveAndFlush(
                new Lead(
                        userA.getId(),
                        "Lead B",
                        "lead_b_" + UUID.randomUUID() + "@mail.com",
                        "111"
                )
        );

        UUID leadId = leadA.getId();

        // ---------- TENANT B ----------
        TenantContext.clear();
        TenantContext.setTenant(tenantB.getSchemaName());

        boolean existsInTenantB = leadRepository.findById(leadId).isPresent();

        assertThat(existsInTenantB)
                .as("Tenant B must not access Tenant A data")
                .isFalse();
    }
}