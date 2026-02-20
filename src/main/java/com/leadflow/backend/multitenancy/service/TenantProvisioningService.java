package com.leadflow.backend.multitenancy.service;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.regex.Pattern;

@Service
public class TenantProvisioningService {

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public TenantProvisioningService(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Transactional(transactionManager = "publicTransactionManager")
    public void provisionTenant(String schemaName) {

        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Schema name cannot be null or blank");
        }

        String schema = schemaName.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("Invalid schema name");
        }

        // Cria schema físico
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"");

        // Executa migrations apenas para o schema do tenant
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}