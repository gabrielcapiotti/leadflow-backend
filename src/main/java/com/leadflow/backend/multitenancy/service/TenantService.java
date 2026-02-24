package com.leadflow.backend.multitenancy.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class TenantService {

    private static final String DEFAULT_SCHEMA = "public";

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]{3,50}$");

    private static final String RESOLVE_SCHEMA_SQL = """
            SELECT schema_name
            FROM public.tenants
            WHERE LOWER(schema_name) = LOWER(?)
              AND deleted_at IS NULL
            """;

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

            String schema = jdbcTemplate.queryForObject(
                    RESOLVE_SCHEMA_SQL,
                    String.class,
                    normalized
            );

            return Optional.ofNullable(schema);

        } catch (EmptyResultDataAccessException ex) {
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