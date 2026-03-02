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
import java.util.regex.Pattern;

@Component
public class MultiTenantConnectionProviderImpl
        implements MultiTenantConnectionProvider<String> {

    private static final Logger log =
            LoggerFactory.getLogger(MultiTenantConnectionProviderImpl.class);

    private static final String DEFAULT_SCHEMA = "public";

    /**
     * Apenas letras minúsculas, números e underscore.
     * Segurança contra injection.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]{1,63}$");

    private final DataSource dataSource;

    public MultiTenantConnectionProviderImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* ======================================================
       BASE CONNECTION
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

    /* ======================================================
       TENANT CONNECTION
       ====================================================== */

    @Override
    public Connection getConnection(String tenantIdentifier)
            throws SQLException {

        Connection connection = getAnyConnection();

        String schema = resolveSchema(tenantIdentifier);

        try {
            switchSchema(connection, schema);
            log.trace("Connection switched to schema: {}", schema);
        } catch (SQLException ex) {
            connection.close();
            throw new SQLException(
                    "Failed to switch schema to: " + schema, ex
            );
        }

        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier,
                                  Connection connection)
            throws SQLException {

        try {
            resetSchema(connection);
        } catch (SQLException ex) {
            log.error("Failed to reset schema to default", ex);
        } finally {
            connection.close();
        }
    }

    /* ======================================================
       SCHEMA SWITCHING
       ====================================================== */

    private void switchSchema(Connection connection, String schema)
            throws SQLException {

        try {
            // Preferível (JDBC 4.1+)
            connection.setSchema(schema);
        } catch (SQLException ex) {
            // Fallback para PostgreSQL
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET search_path TO " + schema);
            }
        }
    }

    private void resetSchema(Connection connection)
            throws SQLException {

        try {
            connection.setSchema(DEFAULT_SCHEMA);
        } catch (SQLException ex) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET search_path TO " + DEFAULT_SCHEMA);
            }
        }
    }

    /* ======================================================
       TENANT VALIDATION
       ====================================================== */

    private String resolveSchema(String tenantIdentifier) {

        if (Objects.isNull(tenantIdentifier)
                || tenantIdentifier.isBlank()) {
            return DEFAULT_SCHEMA;
        }

        String normalized = tenantIdentifier
                .trim()
                .toLowerCase();

        if (!VALID_SCHEMA.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + normalized
            );
        }

        return normalized;
    }

    /* ======================================================
       HIBERNATE CONTRACT
       ====================================================== */

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class
                .isAssignableFrom(unwrapType)
                || MultiTenantConnectionProviderImpl.class
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
}