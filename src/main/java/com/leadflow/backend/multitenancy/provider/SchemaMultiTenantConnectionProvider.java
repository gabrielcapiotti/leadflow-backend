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

        logger.debug("Switching connection to schema: {}", schema);

        try (Statement statement = connection.createStatement()) {

            // Mais seguro que setSchema() no PostgreSQL
            statement.execute("SET search_path TO \"" + schema + "\"");

        } catch (SQLException e) {

            connection.close();

            throw new SQLException(
                    "Failed to switch to schema: " + schema,
                    e
            );
        }

        return connection;
    }

    @Override
    public void releaseConnection(
            String tenantIdentifier,
            Connection connection
    ) throws SQLException {

        try (Statement statement = connection.createStatement()) {

            // Sempre resetar antes de devolver ao pool
            statement.execute("SET search_path TO \"" + DEFAULT_SCHEMA + "\"");

        } catch (SQLException e) {

            logger.error("Failed to reset schema to public", e);

        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false; // importante para evitar problemas com pool
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class
                .isAssignableFrom(unwrapType)
                || SchemaMultiTenantConnectionProvider.class
                .isAssignableFrom(unwrapType);
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
       INTERNAL VALIDATION
       ====================================================== */

    private String resolveSchema(String tenantIdentifier) {

        if (Objects.isNull(tenantIdentifier) ||
                tenantIdentifier.isBlank()) {

            logger.debug("No tenant identifier provided. Using public schema.");
            return DEFAULT_SCHEMA;
        }

        // Proteção contra schema injection
        if (!tenantIdentifier.matches("^[a-zA-Z0-9_]+$")) {

            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenantIdentifier
            );
        }

        return tenantIdentifier.toLowerCase();
    }
}