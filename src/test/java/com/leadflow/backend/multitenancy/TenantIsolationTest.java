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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration")
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

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setup() {
        TenantContext.clear();  // Limpar o contexto de tenant

        tenantA = testTenantFactory.createTenant("Tenant A");
        tenantB = testTenantFactory.createTenant("Tenant B");

        // Criar os roles para garantir que ROLE_USER exista
        if (!roleRepository.existsByNameIgnoreCase("ROLE_USER")) {
            roleRepository.save(new Role("ROLE_USER"));
        }
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();  // Limpar o contexto de tenant após cada teste
    }

    /* ======================================================
       TEST 1 — Isolation via COUNT
       ====================================================== */

    @Test
    void shouldIsolateDataBetweenSchemas() {
        // ---------- TENANT A ----------
        TenantContext.setTenant(tenantA.getSchemaName());

        Role role = roleRepository.findByNameIgnoreCase("ROLE_USER")
                .orElseThrow(() ->
                        new IllegalStateException("Role 'ROLE_USER' not found"));

        User userA = userRepository.saveAndFlush(
                new User(
                        "User A",
                        "a_" + UUID.randomUUID() + "@mail.com",
                        "pass",
                        role
                )
        );

        Lead leadA = new Lead(
                userA.getId(),
                "Lead A",
                "lead_a_" + UUID.randomUUID() + "@mail.com",
                "111"
        );

        Lead savedLead = leadRepository.saveAndFlush(leadA);

        assertThat(savedLead.getId()).isNotNull();

        assertThat(leadRepository.count())
                .as("Tenant A must contain exactly 1 lead")
                .isEqualTo(1);

        // ---------- TENANT B ----------
        TenantContext.clear();
        TenantContext.setTenant(tenantB.getSchemaName());

        assertThat(leadRepository.count())
                .as("Tenant B must not see Tenant A data")
                .isZero();
    }

    /* ======================================================
       TEST 2 — Isolation via Query
       ====================================================== */

    @Test
    void shouldNotAccessOtherTenantData() {
        // ---------- TENANT A ----------
        TenantContext.setTenant(tenantA.getSchemaName());

        Role role = roleRepository.findByNameIgnoreCase("ROLE_USER")
                .orElseThrow(() ->
                        new IllegalStateException("Role 'ROLE_USER' not found"));

        User userA = userRepository.saveAndFlush(
                new User(
                        "User B",
                        "b_" + UUID.randomUUID() + "@mail.com",
                        "pass",
                        role
                )
        );

        Lead leadA = new Lead(
                userA.getId(),
                "Lead A",
                "lead_b_" + UUID.randomUUID() + "@mail.com",
                "111"
        );

        Lead savedLead = leadRepository.saveAndFlush(leadA);

        UUID savedLeadId = savedLead.getId();

        // ---------- TENANT B ----------
        TenantContext.clear();
        TenantContext.setTenant(tenantB.getSchemaName());

        boolean exists = leadRepository.findAll()
                .stream()
                .anyMatch(l -> l.getId().equals(savedLeadId));

        assertThat(exists)
                .as("Tenant B must not access Tenant A data")
                .isFalse();
    }
}