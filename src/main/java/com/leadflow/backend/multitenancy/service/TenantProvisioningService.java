package com.leadflow.backend.multitenancy.service;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.repository.tenant.TenantRepository;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.regex.Pattern;

@Service
@Transactional(transactionManager = "publicTransactionManager")
public class TenantProvisioningService {

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

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

    @Transactional("publicTransactionManager")
    public void provisionTenant(String tenantName) {

        validateTenantName(tenantName);

        String schemaName = normalizeSchema(tenantName);

        if (!VALID_SCHEMA.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name");
        }

        // 🔥 Idempotência
        if (tenantRepository.existsBySchemaNameIgnoreCase(schemaName)) {
            return;
        }

        createSchema(schemaName);
        runMigrations(schemaName);
        registerTenant(tenantName, schemaName);
    }

    /* ================= SCHEMA ================= */

    private void createSchema(String schemaName) {

        jdbcTemplate.execute(
                "CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\""
        );
    }

    /* ================= FLYWAY ================= */

    private void runMigrations(String schemaName) {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }

    /* ================= REGISTER ================= */

    private void registerTenant(String tenantName, String schemaName) {

        Tenant tenant = new Tenant(
                tenantName.trim(),
                schemaName
        );

        tenantRepository.save(tenant);
    }

    /* ================= VALIDATION ================= */

    private void validateTenantName(String tenantName) {

        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }

        if (tenantName.length() > 100) {
            throw new IllegalArgumentException("Tenant name too long");
        }
    }

    private String normalizeSchema(String name) {

        return name
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replace(" ", "_");
    }
}