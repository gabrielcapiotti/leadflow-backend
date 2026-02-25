package com.leadflow.backend.integration;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.repository.tenant.TenantRepository;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-flyway")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlywayIntegrationTest {

    private static final String TENANT_A = "tenant_a";
    private static final String TENANT_B = "tenant_b";

    @Autowired private LeadRepository leadRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Role globalRole;

    /* ======================================================
       SETUP
       ====================================================== */

    @BeforeEach
    void setup() {

        TenantContext.clear();

        // 🔥 Remove schemas se existirem
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TENANT_A + " CASCADE");
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TENANT_B + " CASCADE");

        // 🔥 Limpa tabelas globais com segurança
        jdbcTemplate.execute("TRUNCATE TABLE public.tenants RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE public.roles RESTART IDENTITY CASCADE");

        // 🔥 Cria schemas idempotentes
        jdbcTemplate.execute("CREATE SCHEMA " + TENANT_A);
        jdbcTemplate.execute("CREATE SCHEMA " + TENANT_B);

        // 🔥 Role global única
        globalRole = roleRepository.saveAndFlush(new Role("ROLE_USER"));

        // 🔥 Registra tenants
        tenantRepository.saveAndFlush(new Tenant("Tenant A", TENANT_A));
        tenantRepository.saveAndFlush(new Tenant("Tenant B", TENANT_B));

        // 🔥 Aplica migrations por schema
        migrateSchema(TENANT_A);
        migrateSchema(TENANT_B);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ======================================================
       1️⃣ ISOLATION
       ====================================================== */

    @Test
    void shouldIsolateDataBetweenTenants() {

        TenantContext.setTenant(TENANT_A);

        User userA = userRepository.saveAndFlush(
                new User("A", "a@email.com", "pass", globalRole)
        );

        Lead leadA = leadRepository.saveAndFlush(
                new Lead(userA.getId(), "leadA@email.com", "Lead A", "111")
        );

        UUID leadAId = leadA.getId();

        TenantContext.setTenant(TENANT_B);

        User userB = userRepository.saveAndFlush(
                new User("B", "b@email.com", "pass", globalRole)
        );

        Lead leadB = leadRepository.saveAndFlush(
                new Lead(userB.getId(), "leadB@email.com", "Lead B", "222")
        );

        UUID leadBId = leadB.getId();

        TenantContext.setTenant(TENANT_A);

        assertThat(leadRepository.findById(leadAId)).isPresent();
        assertThat(leadRepository.findById(leadBId)).isEmpty();
    }

    /* ======================================================
       2️⃣ SEARCH_PATH
       ====================================================== */

    @Test
    void shouldSetCorrectSearchPath() {

        TenantContext.setTenant(TENANT_A);

        String searchPath = jdbcTemplate.queryForObject(
                "SHOW search_path",
                String.class
        );

        assertNotNull(searchPath);
        assertTrue(searchPath.contains(TENANT_A));
    }

    /* ======================================================
       3️⃣ UNIQUE PER TENANT
       ====================================================== */

    @Test
    void shouldAllowSameEmailInDifferentTenants() {

        TenantContext.setTenant(TENANT_A);

        userRepository.saveAndFlush(
                new User("A", "duplicate@email.com", "pass", globalRole)
        );

        TenantContext.setTenant(TENANT_B);

        User userB = userRepository.saveAndFlush(
                new User("B", "duplicate@email.com", "pass", globalRole)
        );

        assertNotNull(userB.getId());
    }

    /* ======================================================
       4️⃣ SOFT DELETE
       ====================================================== */

    @Test
    void shouldSoftDeleteWithoutAffectingOtherTenant() {

        TenantContext.setTenant(TENANT_A);

        User userA = userRepository.saveAndFlush(
                new User("A", "soft@email.com", "pass", globalRole)
        );

        Lead leadA = leadRepository.saveAndFlush(
                new Lead(userA.getId(), "softlead@email.com", "Lead A", "111")
        );

        UUID leadAId = leadA.getId();

        leadA.softDelete();
        leadRepository.saveAndFlush(leadA);

        assertThat(
                leadRepository.findByIdAndUserIdAndDeletedAtIsNull(
                        leadAId,
                        userA.getId()
                )
        ).isEmpty();

        TenantContext.setTenant(TENANT_B);

        assertThat(leadRepository.findById(leadAId)).isEmpty();
    }

    /* ======================================================
       5️⃣ FLYWAY HISTORY
       ====================================================== */

    @Test
    void shouldApplyFlywayToTenantSchema() {

        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TENANT_A + ".flyway_schema_history",
                Integer.class
        );

        assertNotNull(migrationCount);
        assertTrue(migrationCount > 0);
    }

    /* ======================================================
       HELPER
       ====================================================== */

    private void migrateSchema(String schema) {

        Flyway tenantFlyway = Flyway.configure()
                .dataSource(flyway.getConfiguration().getDataSource())
                .locations("classpath:db/migration/tenant")
                .schemas(schema)
                .defaultSchema(schema)
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .load();

        tenantFlyway.migrate();
    }
}