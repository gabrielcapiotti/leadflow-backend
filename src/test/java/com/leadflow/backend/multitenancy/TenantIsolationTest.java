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
    private TenantRepository tenantRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestTenantFactory testTenantFactory;

    @BeforeEach
    void setup() {
        TenantContext.clear();
        testTenantFactory.createTenant("Tenant A");
        testTenantFactory.createTenant("Tenant B");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ======================================================
       TEST 1 — Isolation via COUNT
       ====================================================== */

    @Test
    void shouldIsolateDataBetweenSchemas() {

        // ---------- TENANT A ----------
        TenantContext.setTenant("tenant_a");

        Tenant tenantA = tenantRepository.findByNameIgnoreCase("Tenant A")
                .orElseThrow(() ->
                        new IllegalStateException("Tenant 'Tenant A' not found"));

        Role role = roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() ->
                        new IllegalStateException("Role 'USER' not found"));

        User userA = userRepository.save(
                new User(
                        "User A",
                        "a_" + UUID.randomUUID() + "@mail.com",
                        "pass",
                        role,
                        tenantA
                )
        );

        Lead leadA = new Lead(
                "Lead A",
                "lead_a_" + UUID.randomUUID() + "@mail.com",
                "111"
        );

        leadA.setUser(userA);
        leadA.setTenant(tenantA);

        leadRepository.saveAndFlush(leadA);

        assertThat(leadRepository.count())
                .as("Tenant A must contain exactly 1 lead")
                .isEqualTo(1);

        // ---------- TENANT B ----------
        TenantContext.clear();
        TenantContext.setTenant("tenant_b");

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
        TenantContext.setTenant("tenant_a");

        Tenant tenantA = tenantRepository.findByNameIgnoreCase("Tenant A")
                .orElseThrow(() ->
                        new IllegalStateException("Tenant 'Tenant A' not found"));

        Role role = roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() ->
                        new IllegalStateException("Role 'USER' not found"));

        User userA = userRepository.save(
                new User(
                        "User B",
                        "b_" + UUID.randomUUID() + "@mail.com",
                        "pass",
                        role,
                        tenantA
                )
        );

        Lead leadA = new Lead(
                "Lead A",
                "lead_b_" + UUID.randomUUID() + "@mail.com",
                "111"
        );

        leadA.setUser(userA);
        leadA.setTenant(tenantA);

        leadRepository.saveAndFlush(leadA);

        // ---------- TENANT B ----------
        TenantContext.clear();
        TenantContext.setTenant("tenant_b");

        boolean exists = leadRepository.findAll()
                .stream()
                .anyMatch(l -> l.getName().equals("Lead A"));

        assertThat(exists)
                .as("Tenant B must not access Tenant A data")
                .isFalse();
    }
}