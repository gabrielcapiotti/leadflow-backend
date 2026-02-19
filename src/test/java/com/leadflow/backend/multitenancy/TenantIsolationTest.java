package com.leadflow.backend.multitenancy;

import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.tenant.TenantRepository;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;

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
    private TenantRepository tenantRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        TenantContext.clear();

        // Cria schemas se ainda não existirem
        tenantService.createTenantSchema("tenant_a");
        tenantService.createTenantSchema("tenant_b");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ==========================
       TESTE 1 - ISOLAMENTO POR COUNT
       ========================== */

    @Test
    void shouldIsolateDataBetweenSchemas() {

        // ===== TENANT A =====
        TenantContext.setTenant("tenant_a");

        Tenant tenant = tenantRepository.findByNameIgnoreCase("Tenant A")
                .orElseThrow(() -> new IllegalStateException("Tenant 'Tenant A' não encontrado no seed"));

        Role role = roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new IllegalStateException("Role 'USER' não encontrada no seed"));

        User user = userRepository.save(
                new User(
                        "User A",
                        "a_" + UUID.randomUUID() + "@mail.com",
                        "pass",
                        role,
                        tenant
                )
        );

        Lead leadA = new Lead(
                "Lead A",
                "lead_a_" + UUID.randomUUID() + "@mail.com",
                "111"
        );

        leadA.setUser(user);
        leadA.setTenant(tenant);

        leadRepository.saveAndFlush(leadA);

        assertThat(leadRepository.count()).isEqualTo(1);

        // ===== TENANT B =====
        TenantContext.setTenant("tenant_b");

        assertThat(leadRepository.count())
                .as("Tenant B não deve enxergar dados do tenant A")
                .isZero();
    }

    /* ==========================
       TESTE 2 - ISOLAMENTO POR CONSULTA
       ========================== */

    @Test
    void shouldNotAccessOtherTenantData() {

        // ===== TENANT A =====
        TenantContext.setTenant("tenant_a");

        Tenant tenant = tenantRepository.findByNameIgnoreCase("Tenant A")
                .orElseThrow(() -> new IllegalStateException("Tenant 'Tenant A' não encontrado no seed"));

        Role role = roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new IllegalStateException("Role 'USER' não encontrada no seed"));

        User user = userRepository.save(
                new User(
                        "User B",
                        "b_" + UUID.randomUUID() + "@mail.com",
                        "pass",
                        role,
                        tenant
                )
        );

        Lead leadA = new Lead(
                "Lead A",
                "lead_b_" + UUID.randomUUID() + "@mail.com",
                "111"
        );

        leadA.setUser(user);
        leadA.setTenant(tenant);

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
