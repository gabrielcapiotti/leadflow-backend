package com.leadflow.backend.multitenancy.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;

@Service
public class TenantService {

    private static final String DEFAULT_SCHEMA = "public";

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;

    public TenantService(JdbcTemplate jdbcTemplate, Flyway flyway) {
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
    }

    /* ======================================================
       RESOLVE TENANT → SCHEMA
       ====================================================== */

    public String resolveSchemaByTenantId(String tenantId) {

        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }

        String normalized = tenantId.trim().toLowerCase();

        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT schema_name
                    FROM tenants
                    WHERE LOWER(name) = LOWER(?)
                      AND deleted_at IS NULL
                    """,
                    String.class,
                    normalized
            );
        } catch (Exception ex) {
            return null;
        }
    }

    /* ======================================================
       CREATE SCHEMA
       ====================================================== */

    public void createTenantSchema(String schemaName) {

        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Schema name cannot be null or blank");
        }

        String schema = schemaName.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("Invalid schema name");
        }

        jdbcTemplate.execute(
                "CREATE SCHEMA IF NOT EXISTS \"" + schema + "\""
        );

        // Configure Flyway for the new schema and apply migrations
        flyway.configure().schemas(schema).load().migrate();
    }

    /* ======================================================
       CREATE TENANT (REGISTRO + SCHEMA)
       ====================================================== */

    public void createTenant(String name, String schemaName) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }

        createTenantSchema(schemaName);

        jdbcTemplate.update(
                """
                INSERT INTO tenants (name, schema_name, created_at, updated_at)
                VALUES (?, ?, NOW(), NOW())
                ON CONFLICT (schema_name) DO NOTHING
                """,
                name.trim(),
                schemaName.trim().toLowerCase()
        );
    }

    /* ======================================================
       DEFAULT FALLBACK
       ====================================================== */

    public String getDefaultSchema() {
        return DEFAULT_SCHEMA;
    }
}
