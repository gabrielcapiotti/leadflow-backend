package com.leadflow.backend.service;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.repository.tenant.TenantRepository;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Service
@Transactional(transactionManager = "publicTransactionManager")
public class TenantProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final TenantRepository tenantRepository;

    public TenantProvisioningService(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource,
            TenantRepository tenantRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.tenantRepository = tenantRepository;
    }

    /* ======================================================
       MAIN ENTRY POINT
       ====================================================== */

    @Transactional("publicTransactionManager")
    public void provisionTenant(String tenantName) {

        validateTenantName(tenantName);

        String schemaName = normalizeSchema(tenantName);

        // Idempotência: se já existe no banco, não recria
        if (tenantRepository.existsBySchemaNameIgnoreCase(schemaName)) {
            return;
        }

        createSchema(schemaName);
        runMigrations(schemaName); // Adiciona as tabelas no schema do tenant
        registerTenant(tenantName, schemaName);
    }

    /* ======================================================
       SCHEMA CREATION
       ====================================================== */

    private void createSchema(String schemaName) {

        if (!schemaName.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid schema name");
        }

        // Evita SQL Injection via regex + aspas
        jdbcTemplate.execute(
                "CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\""
        );
    }

    /* ======================================================
       FLYWAY MIGRATION
       ====================================================== */

    private void runMigrations(String schemaName) {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate(); // Executa as migrations no schema do tenant
    }

    /* ======================================================
       REGISTER TENANT
       ====================================================== */

    private void registerTenant(String tenantName, String schemaName) {

        Tenant tenant = new Tenant(
                tenantName.trim(),
                schemaName
        );

        tenantRepository.save(tenant);
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void validateTenantName(String tenantName) {

        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }

        if (tenantName.length() > 100) {
            throw new IllegalArgumentException("Tenant name too long");
        }
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private String normalizeSchema(String name) {

        return name
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")  // remove caracteres perigosos
                .replace(" ", "_");
    }
}
