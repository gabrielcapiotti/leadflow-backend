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

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Tenant tenantA;
    private Tenant tenantB;

    /* ======================================================
       SETUP
       ====================================================== */

    @BeforeEach
    void setup() {

        TenantContext.clear();
        entityManager.clear();

        tenantRepository.deleteAll();

        tenantA = testTenantFactory.createTenant(
                "tenant_a_" + UUID.randomUUID().toString().replace("-", "")
        );

        tenantB = testTenantFactory.createTenant(
                "tenant_b_" + UUID.randomUUID().toString().replace("-", "")
        );

        createSchema(tenantA.getSchemaName());
        createSchema(tenantB.getSchemaName());

        ensureRoleExists("ROLE_USER");

        initializeTenant(tenantA.getSchemaName());
        initializeTenant(tenantB.getSchemaName());

        assertTenantTablesExist(tenantA.getSchemaName());
        assertTenantTablesExist(tenantB.getSchemaName());

        TenantContext.clear();
        entityManager.clear();
    }

    @AfterEach
    void cleanup() {

        TenantContext.clear();
        entityManager.clear();
    }

    /* ======================================================
       SCHEMA MANAGEMENT
       ====================================================== */

    private void createSchema(String schema) {

        jdbcTemplate.execute(
                "CREATE SCHEMA IF NOT EXISTS \"" + schema + "\""
        );
    }

    private Role ensureRoleExists(String roleName) {

        TenantContext.setTenant("public");

        Role role = roleRepository
                .findByNameIgnoreCase(roleName)
                .orElseGet(() ->
                        roleRepository.saveAndFlush(new Role(roleName))
                );

        entityManager.clear();
        TenantContext.clear();

        return role;
    }

    private void initializeTenant(String schemaName) {

        // CRITICAL: Create schema first
        String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"";
        jdbcTemplate.execute(createSchemaSQL);
        System.out.println(">>> Created schema: " + schemaName);

        // Wrap ALL table creation in a single transaction to keep same connection
        transactionTemplate.executeWithoutResult(status -> {
            // Set search_path and create tables in SAME transaction/connection
            jdbcTemplate.execute("SET search_path TO \"" + schemaName + "\", public");
            System.out.println(">>> Set search_path to: " + schemaName);

            String createRolesSQL = """
                CREATE TABLE IF NOT EXISTS roles (
                    id UUID PRIMARY KEY,
                    name VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
            """;
            
            jdbcTemplate.execute(createRolesSQL);
            System.out.println(">>> Created roles table in: " + schemaName);

            String createUsersSQL = """
                CREATE TABLE IF NOT EXISTS users (
                    id UUID PRIMARY KEY,
                    name VARCHAR(255),
                    email VARCHAR(255),
                    password VARCHAR(255),
                    role_id UUID,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    deleted_at TIMESTAMP,
                    credentials_updated_at TIMESTAMP,
                    failed_attempts INTEGER DEFAULT 0,
                    lock_until TIMESTAMP
                )
            """;
            
            jdbcTemplate.execute(createUsersSQL);
            System.out.println(">>> Created users table in: " + schemaName);

            String createLeadsSQL = """
                CREATE TABLE IF NOT EXISTS leads (
                    id UUID PRIMARY KEY,
                    user_id UUID NOT NULL,
                    name VARCHAR(255),
                    email VARCHAR(255),
                    phone VARCHAR(50),
                    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    deleted_at TIMESTAMP
                )
            """;
            
            jdbcTemplate.execute(createLeadsSQL);
            System.out.println(">>> Created leads table in: " + schemaName);

            // Reset search_path before returning connection to pool
            jdbcTemplate.execute("SET search_path TO public");
        });

        // After transaction completes, verify tables exist
        var tablesList = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                """,
                String.class,
                schemaName
        );
        System.out.println(">>> Tables in schema " + schemaName + ": " + tablesList);

        // Debug: check ALL tables to see where they're being created
        var allTables = jdbcTemplate.queryForList(
                """
                SELECT table_schema, table_name  
                FROM information_schema.tables
                WHERE table_name IN ('roles', 'users', 'leads')
                """
        );
        System.out.println(">>> ALL roles/users/leads tables in DB: " + allTables);
    }

        private void assertTenantTablesExist(String schemaName) {

                List<String> tables = jdbcTemplate.queryForList(
                                """
                                SELECT table_name
                                FROM information_schema.tables
                                WHERE table_schema = ?
                                    AND table_name IN ('roles', 'users', 'leads')
                                """,
                                String.class,
                                schemaName
                );

                assertThat(tables)
                                .as("Expected tenant schema '%s' to have roles/users/leads tables", schemaName)
                                .containsExactlyInAnyOrder("roles", "users", "leads");
        }

    private void setTenant(String schema) {

        TenantContext.clear();
        entityManager.clear();

        TenantContext.setTenant(schema);

        // força o schema da conexão atual
        transactionTemplate.executeWithoutResult(status ->
                jdbcTemplate.execute("SET search_path TO \"" + schema + "\"")
        );
    }

    /* ======================================================
       TESTS
       ====================================================== */

    @Test
    void shouldIsolateDataBetweenSchemas() {

        TenantContext.setTenant("public");

        Role role = roleRepository
                .findByNameIgnoreCase("ROLE_USER")
                .orElseThrow();

        entityManager.clear();

        setTenant(tenantA.getSchemaName());

        transactionTemplate.executeWithoutResult(status -> {

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

            assertThat(leadRepository.count()).isEqualTo(1);
        });

        entityManager.clear();
        TenantContext.clear();

        setTenant(tenantB.getSchemaName());

        Long count = transactionTemplate.execute(status ->
                leadRepository.count()
        );

        assertThat(count).isZero();
    }

    @Test
    void shouldNotAccessOtherTenantData() {

        TenantContext.setTenant("public");

        Role role = roleRepository
                .findByNameIgnoreCase("ROLE_USER")
                .orElseThrow();

        entityManager.clear();

        setTenant(tenantA.getSchemaName());

        UUID leadId = transactionTemplate.execute(status -> {

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

            return Objects.requireNonNull(leadA.getId());
        });

        entityManager.clear();
        TenantContext.clear();

        setTenant(tenantB.getSchemaName());

        assertThat(
                leadRepository.findById(leadId)
        ).isEmpty();
    }
}