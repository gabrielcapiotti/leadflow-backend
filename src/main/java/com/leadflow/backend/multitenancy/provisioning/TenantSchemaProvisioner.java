package com.leadflow.backend.multitenancy.provisioning;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

@Service
public class TenantSchemaProvisioner {

    private final DataSource dataSource;

    public TenantSchemaProvisioner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String provisionTenantSchema(UUID vendorId) {

        String schemaName = "tenant_" + vendorId.toString().replace("-", "");

        createSchema(schemaName);
        runMigrations(schemaName);

        return schemaName;
    }

    private void createSchema(String schemaName) {

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        } catch (Exception e) {

            throw new RuntimeException("Erro ao criar schema do tenant", e);
        }
    }

    private void runMigrations(String schemaName) {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}