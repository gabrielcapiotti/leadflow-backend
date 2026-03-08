package com.leadflow.backend.util;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.tenant.TenantRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class TestTenantFactory {

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z][a-z0-9_]*$");

    private final TenantRepository tenantRepository;
    private final JdbcTemplate jdbcTemplate;

    public TestTenantFactory(
            TenantRepository tenantRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.tenantRepository = tenantRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /* ======================================================
       CREATE TENANT
       ====================================================== */

    @Transactional
    public Tenant createTenant(String tenantName) {

        if (!StringUtils.hasText(tenantName)) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }

        String normalizedName = tenantName.trim().toLowerCase();
        String schema = normalizeSchema(normalizedName);

        /*
         * Verifica se tenant já existe
         */
        return tenantRepository
                .findBySchemaNameIgnoreCase(schema)
                .orElseGet(() -> createNewTenant(normalizedName, schema));
    }

    private Tenant createNewTenant(String normalizedName, String schema) {

        createSchemaIfNotExists(schema);

        Tenant tenant = new Tenant(
                normalizedName,
                schema
        );

        return tenantRepository.save(tenant);
    }

    private void createSchemaIfNotExists(String schema) {

        if (!VALID_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        }

        jdbcTemplate.execute(
                "CREATE SCHEMA IF NOT EXISTS \"" + schema + "\""
        );
    }

    /* ======================================================
       CREATE + ACTIVATE
       ====================================================== */

    @Transactional
    public Tenant createAndActivate(String tenantName) {

        Tenant tenant = createTenant(tenantName);

        TenantContext.setTenant(tenant.getSchemaName());

        return tenant;
    }

    /* ======================================================
       CONTEXT CONTROL
       ====================================================== */

    public void setTenantContext(String schemaName) {

        if (!StringUtils.hasText(schemaName)) {
            throw new IllegalArgumentException("Schema name cannot be blank");
        }

        String normalized = normalizeSchema(schemaName);

        TenantContext.setTenant(normalized);
    }

    public void clear() {
        TenantContext.clear();
    }

    /* ======================================================
       NORMALIZATION
       ====================================================== */

    private String normalizeSchema(String name) {

        String schema = name
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", "_");

        if (!StringUtils.hasText(schema)) {
            throw new IllegalArgumentException("Invalid tenant name after normalization");
        }

        if (Character.isDigit(schema.charAt(0))) {
            schema = "t_" + schema;
        }

        return schema;
    }
}