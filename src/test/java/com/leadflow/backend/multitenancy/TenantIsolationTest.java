package com.leadflow.backend.multitenancy;

import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.repository.lead.LeadRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("integration")
@Tag("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TenantIsolationTest extends IntegrationTestBase {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private LeadRepository leadRepository;

    @BeforeEach
    void setup() {
        TenantContext.clear();

        tenantService.createTenantSchema("tenant_a");
        tenantService.createTenantSchema("tenant_b");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ==========================
       CONTEXT
       ========================== */

    @Test
    void shouldSetAndRetrieveTenantCorrectly() {
        TenantContext.setTenant("tenant_a");

        assertThat(TenantContext.getTenant())
                .isEqualTo("tenant_a");
    }

    @Test
    void shouldDefaultToNullWhenTenantNotSet() {
        assertThat(TenantContext.getTenant())
                .isNull();
    }

    @Test
    void shouldClearTenantContext() {
        TenantContext.setTenant("tenant_a");
        TenantContext.clear();

        assertThat(TenantContext.getTenant())
                .isNull();
    }

    /* ==========================
       ISOLATION
       ========================== */

    @Test
    void shouldIsolateDataBetweenSchemas() {

        // ===== TENANT A =====
        TenantContext.setTenant("tenant_a");

        Lead leadA = new Lead(
                "Lead A",
                "a_" + UUID.randomUUID() + "@email.com",
                "111"
        );

        leadRepository.saveAndFlush(leadA);

        assertThat(leadRepository.count()).isEqualTo(1);

        // ===== TENANT B =====
        TenantContext.setTenant("tenant_b");

        assertThat(leadRepository.count())
                .as("Tenant B não deve enxergar dados do tenant A")
                .isZero();
    }

    @Test
    void shouldNotAccessOtherTenantData() {

        // ===== TENANT A =====
        TenantContext.setTenant("tenant_a");

        Lead leadA = new Lead(
                "Lead A",
                "b_" + UUID.randomUUID() + "@email.com",
                "111"
        );

        leadRepository.saveAndFlush(leadA);

        // ===== TENANT B =====
        TenantContext.setTenant("tenant_b");

        boolean exists = leadRepository.findAll()
                .stream()
                .anyMatch(l -> l.getName().equals("Lead A"));

        assertThat(exists)
                .as("Dados de tenant_a não devem estar acessíveis em tenant_b")
                .isFalse();
    }
}
