package com.leadflow.backend.multitenancy.service;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class MultiTenancyProvisioningService {

    private static final Pattern TENANT_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    private final DataSource dataSource;

    public MultiTenancyProvisioningService(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    /**
     * Cria o schema do tenant e executa as migrations.
     */
    public void provision(String tenantIdentifier) {

        validateTenantIdentifier(tenantIdentifier);

        createSchema(tenantIdentifier);

        runMigrations(tenantIdentifier);
    }

    private void createSchema(String tenantIdentifier) {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(
                    "CREATE SCHEMA IF NOT EXISTS " + tenantIdentifier
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to create schema for tenant: " + tenantIdentifier,
                    ex
            );
        }
    }

    private void runMigrations(String tenantIdentifier) {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(tenantIdentifier)
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }

    private void validateTenantIdentifier(String tenantIdentifier) {

        if (tenantIdentifier == null || tenantIdentifier.isBlank()) {
            throw new IllegalArgumentException("Tenant identifier cannot be empty");
        }

        if (!TENANT_PATTERN.matcher(tenantIdentifier).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenantIdentifier
            );
        }
    }
}