package com.leadflow.backend.multitenancy.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class TenantService {

    private static final String DEFAULT_SCHEMA = "public";

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]{3,50}$");

    private final JdbcTemplate jdbcTemplate;

    public TenantService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /* ======================================================
       RESOLVE TENANT → SCHEMA
       ====================================================== */

    public Optional<String> resolveSchemaByTenantIdentifier(String identifier) {

        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String normalized = identifier.trim().toLowerCase();

        try {
            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(
                            """
                            SELECT schema_name
                            FROM tenants
                            WHERE LOWER(schema_name) = LOWER(?)
                              AND deleted_at IS NULL
                            """,
                            String.class,
                            normalized
                    )
            );
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /* ======================================================
       VALIDATE SCHEMA NAME
       ====================================================== */

    public void validateSchemaName(String schema) {

        if (schema == null || schema.isBlank()) {
            throw new IllegalArgumentException("Schema cannot be blank");
        }

        String normalized = schema.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid schema format");
        }
    }

    /* ======================================================
       DEFAULT FALLBACK
       ====================================================== */

    public String getDefaultSchema() {
        return DEFAULT_SCHEMA;
    }
}