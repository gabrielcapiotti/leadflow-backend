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
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TenantProvisioningService {

    private static final Logger logger =
            LoggerFactory.getLogger(TenantProvisioningService.class);

    /**
     * Apenas letras minúsculas, números e underscore.
     * Entre 3 e 50 caracteres.
     */
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

    /* ======================================================
       PUBLIC API
       ====================================================== */

    /**
     * Provisiona um novo tenant baseado em SCHEMA.
     *
     * Processo:
     * 1. Normaliza nome
     * 2. Valida formato
     * 3. Cria schema
     * 4. Executa migrations
     * 5. Registra tenant no schema public
     */
    public synchronized void provisionTenant(String tenantName) {

        validateTenantName(tenantName);

        String schemaName = normalizeSchema(tenantName);

        validateSchemaFormat(schemaName);

        if (tenantRepository.existsBySchemaNameIgnoreCaseAndDeletedAtIsNull(schemaName)) {
            logger.info("Tenant already provisioned: {}", schemaName);
            return;
        }

        try {

            createSchema(schemaName);

            runTenantMigrations(schemaName);

            registerTenantTransactional(tenantName, schemaName);

            logger.info("Tenant successfully provisioned: {}", schemaName);

        } catch (Exception ex) {

            logger.error("Tenant provisioning failed for schema {}", schemaName, ex);

            cleanupSchema(schemaName);

            throw new IllegalStateException(
                    "Tenant provisioning failed for schema: " + schemaName,
                    ex
            );
        }
    }

    /* ======================================================
       SCHEMA MANAGEMENT
       ====================================================== */

    private void createSchema(String schemaName) {

        /*
         * Quote identifier para segurança.
         * Evita problemas com caracteres especiais.
         */
        String sql = "CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"";

        jdbcTemplate.execute(sql);

        logger.debug("Schema created: {}", schemaName);
    }

    private void cleanupSchema(String schemaName) {

        try {

            String sql = "DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE";

            jdbcTemplate.execute(sql);

            logger.warn("Schema rolled back: {}", schemaName);

        } catch (Exception ex) {

            logger.error("Failed to cleanup schema {}", schemaName, ex);
        }
    }

    /* ======================================================
       FLYWAY
       ====================================================== */

    private void runTenantMigrations(String schemaName) {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();

        flyway.migrate();

        logger.debug("Tenant migrations executed for schema: {}", schemaName);
    }

    /* ======================================================
       TENANT REGISTRATION (PUBLIC SCHEMA)
       ====================================================== */

    @Transactional("publicTransactionManager")
    protected void registerTenantTransactional(
            String tenantName,
            String schemaName
    ) {

        Tenant tenant = new Tenant(
                tenantName.trim(),
                schemaName
        );

        tenantRepository.save(tenant);

        logger.debug("Tenant registered in public schema: {}", schemaName);
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void validateTenantName(String tenantName) {

        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant name cannot be null or blank"
            );
        }

        if (tenantName.length() > 100) {
            throw new IllegalArgumentException(
                    "Tenant name exceeds maximum length (100)"
            );
        }
    }

    private void validateSchemaFormat(String schemaName) {

        if (!VALID_SCHEMA.matcher(schemaName).matches()) {
            throw new IllegalArgumentException(
                    "Invalid schema format. Allowed: lowercase letters, numbers and underscore (3–50 chars)"
            );
        }
    }

    private String normalizeSchema(String name) {

        return name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", "")
                .replace(" ", "_");
    }
}