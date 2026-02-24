package com.leadflow.backend.multitenancy.provider;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

@Component
public class SchemaMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String> {

    private static final Logger logger =
            LoggerFactory.getLogger(SchemaMultiTenantConnectionProvider.class);

    private static final String DEFAULT_SCHEMA = "public";

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* ======================================================
       HIBERNATE REQUIRED METHODS
       ====================================================== */

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection)
            throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier)
            throws SQLException {

        Connection connection = getAnyConnection();
        String schema = resolveSchema(tenantIdentifier);

        try {

            ensureSchemaExists(connection, schema);

            // 🔥 Hibernate 6 compatible approach
            connection.setSchema(schema);

            logger.trace("Connection switched to schema: {}", schema);

            return connection;

        } catch (SQLException ex) {

            connection.close();

            throw new SQLException(
                    "Failed to obtain connection for schema: " + schema,
                    ex
            );
        }
    }

    @Override
    public void releaseConnection(
            String tenantIdentifier,
            Connection connection
    ) throws SQLException {

        try {

            // Reset before returning to pool
            connection.setSchema(DEFAULT_SCHEMA);

        } catch (SQLException ex) {

            logger.error("Failed to reset schema to default", ex);

        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType)
                || SchemaMultiTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new IllegalArgumentException(
                "Cannot unwrap to: " + unwrapType
        );
    }

    /* ======================================================
       INTERNAL LOGIC
       ====================================================== */

    private String resolveSchema(String tenantIdentifier) {

        if (Objects.isNull(tenantIdentifier) ||
                tenantIdentifier.isBlank()) {

            return DEFAULT_SCHEMA;
        }

        if (!tenantIdentifier.matches("^[a-z0-9_]+$")) {

            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenantIdentifier
            );
        }

        return tenantIdentifier.toLowerCase();
    }

    /**
     * Garante que o schema exista.
     * Necessário para Testcontainers e DataJpaTest.
     */
    private void ensureSchemaExists(Connection connection, String schema)
            throws SQLException {

        if (DEFAULT_SCHEMA.equals(schema)) {
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        }
    }
}