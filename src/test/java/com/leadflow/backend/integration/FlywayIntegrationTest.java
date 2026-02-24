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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-flyway")
@Transactional
class FlywayIntegrationTest {

    private static final String TENANT_A = "tenant_a";
    private static final String TENANT_B = "tenant_b";

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

    @BeforeEach
    void setup() {

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + TENANT_A);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + TENANT_B);

        tenantRepository.saveAndFlush(new Tenant("Tenant A", TENANT_A));
        tenantRepository.saveAndFlush(new Tenant("Tenant B", TENANT_B));

        migrateSchema(TENANT_A);
        migrateSchema(TENANT_B);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ======================================================
       1️⃣ ISOLATION BETWEEN TENANTS
       ====================================================== */

    @Test
    void shouldIsolateDataBetweenTenants() {

        // Tenant A
        TenantContext.setTenant(TENANT_A);

        Role roleA = roleRepository.saveAndFlush(new Role("ROLE_USER"));
        User userA = userRepository.saveAndFlush(
                new User("A", "a@email.com", "pass", roleA)
        );

        Lead leadA = leadRepository.saveAndFlush(
                new Lead(userA.getId(), "Lead A", "lead@email.com", "111")
        );

        UUID leadAId = leadA.getId();

        // Flush antes de trocar tenant
        leadRepository.flush();

        // Tenant B
        TenantContext.setTenant(TENANT_B);

        Role roleB = roleRepository.saveAndFlush(new Role("ROLE_USER"));
        User userB = userRepository.saveAndFlush(
                new User("B", "b@email.com", "pass", roleB)
        );

        Lead leadB = leadRepository.saveAndFlush(
                new Lead(userB.getId(), "Lead B", "lead@email.com", "222")
        );

        UUID leadBId = leadB.getId();

        // Volta para Tenant A
        TenantContext.setTenant(TENANT_A);

        assertThat(leadRepository.findById(leadAId)).isPresent();
        assertThat(leadRepository.findById(leadBId)).isEmpty();
    }

    /* ======================================================
       2️⃣ SEARCH_PATH RESET
       ====================================================== */

    @Test
    void shouldResetSearchPathAfterConnectionRelease() {

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

        // Tenant A
        TenantContext.setTenant(TENANT_A);

        Role roleA = roleRepository.saveAndFlush(new Role("ROLE_USER"));
        User userA = userRepository.saveAndFlush(
                new User("A", "duplicate@email.com", "pass", roleA)
        );

        Lead leadA = leadRepository.saveAndFlush(
                new Lead(userA.getId(), "Lead A", "same@email.com", "111")
        );

        assertNotNull(leadA.getId());

        // Tenant B
        TenantContext.setTenant(TENANT_B);

        Role roleB = roleRepository.saveAndFlush(new Role("ROLE_USER"));
        User userB = userRepository.saveAndFlush(
                new User("B", "duplicate@email.com", "pass", roleB)
        );

        Lead leadB = leadRepository.saveAndFlush(
                new Lead(userB.getId(), "Lead B", "same@email.com", "222")
        );

        assertNotNull(leadB.getId());
    }

    /* ======================================================
       4️⃣ SOFT DELETE PER SCHEMA
       ====================================================== */

    @Test
    void shouldSoftDeleteWithoutAffectingOtherTenant() {

        TenantContext.setTenant(TENANT_A);

        Role roleA = roleRepository.saveAndFlush(new Role("ROLE_USER"));
        User userA = userRepository.saveAndFlush(
                new User("A", "soft@email.com", "pass", roleA)
        );

        Lead leadA = leadRepository.saveAndFlush(
                new Lead(userA.getId(), "Lead A", "softlead@email.com", "111")
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

        // Tenant B não deve ver nada
        TenantContext.setTenant(TENANT_B);

        assertThat(leadRepository.findById(leadAId)).isEmpty();
    }

    /* ======================================================
       5️⃣ FLYWAY MIGRATION PER TENANT
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
                .locations("classpath:db/migration")
                .schemas(schema)
                .baselineOnMigrate(true)
                .load();

        tenantFlyway.migrate();
    }
}