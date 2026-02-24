package com.leadflow.backend.multitenancy.service;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.repository.tenant.TenantRepository;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.regex.Pattern;

@Service
@Transactional(transactionManager = "publicTransactionManager")
public class TenantProvisioningService {

    private static final Logger logger =
            LoggerFactory.getLogger(TenantProvisioningService.class);

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]{3,50}$");

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

    public synchronized void provisionTenant(String tenantName) {

        validateTenantName(tenantName);

        String schemaName = normalizeSchema(tenantName);

        if (!VALID_SCHEMA.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name format");
        }

        if (tenantRepository.existsBySchemaNameIgnoreCase(schemaName)) {
            logger.info("Tenant already provisioned: {}", schemaName);
            return;
        }

        try {

            createSchema(schemaName);

            runTenantMigrations(schemaName);

            registerTenant(tenantName, schemaName);

            logger.info("Tenant successfully provisioned: {}", schemaName);

        } catch (Exception e) {

            logger.error("Tenant provisioning failed for schema {}",
                    schemaName, e);

            cleanupSchema(schemaName);

            throw new IllegalStateException(
                    "Tenant provisioning failed", e
            );
        }
    }

    /* ================= SCHEMA ================= */

    private void createSchema(String schemaName) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    }

    private void cleanupSchema(String schemaName) {
        try {
            jdbcTemplate.execute(
                    "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE"
            );
            logger.warn("Rolled back schema {}", schemaName);
        } catch (Exception ex) {
            logger.error("Failed to cleanup schema {}", schemaName, ex);
        }
    }

    /* ================= FLYWAY (TENANT ONLY) ================= */

    private void runTenantMigrations(String schemaName) {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration/tenant")
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