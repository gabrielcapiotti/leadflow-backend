package com.leadflow.backend.util;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.tenant.TenantRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TestTenantFactory {

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

    public Tenant createTenant(String tenantName) {

        if (!StringUtils.hasText(tenantName)) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }

        String normalizedName = tenantName.trim().toLowerCase();

        /*
         * Normaliza para schema seguro
         */
        String schema = normalizedName
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", "_");

        if (!StringUtils.hasText(schema)) {
            throw new IllegalArgumentException("Invalid tenant name after normalization");
        }

        /*
         * Cria schema físico no banco
         * Uso de aspas evita problemas com nomes reservados
         */
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"");

        /*
         * Persiste tenant no schema public
         */
        Tenant tenant = new Tenant(
                normalizedName,
                schema
        );

        return tenantRepository.save(tenant);
    }

    /* ======================================================
       CREATE + ACTIVATE
       ====================================================== */

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

        String normalized = schemaName
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "");

        TenantContext.setTenant(normalized);
    }

    public void clear() {
        TenantContext.clear();
    }
}