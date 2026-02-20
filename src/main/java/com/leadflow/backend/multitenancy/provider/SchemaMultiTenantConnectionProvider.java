package com.leadflow.backend.multitenancy.provider;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

@Component
public class SchemaMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String> {

    private static final Logger logger =
            LoggerFactory.getLogger(SchemaMultiTenantConnectionProvider.class);

    private static final String DEFAULT_SCHEMA = "public";

    /**
     * Schema permitido:
     * - letras minúsculas
     * - números
     * - underscore
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* ======================================================
       REQUIRED BY HIBERNATE 6
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

        logger.debug("Getting connection for tenant: {}", tenantIdentifier);

        Connection connection = getAnyConnection();

        try {
            connection.createStatement()
                    .execute("SET search_path TO \"" + tenantIdentifier + "\"");
        } catch (SQLException e) {
            connection.close();
            throw new SQLException("Could not set schema to " + tenantIdentifier, e);
        }

        return connection;
    }

    @Override
    public void releaseConnection(
            String tenantIdentifier,
            Connection connection
    ) throws SQLException {

        try {
            connection.createStatement().execute("SET search_path TO public");
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
        return MultiTenantConnectionProvider.class
                .isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new IllegalArgumentException(
                "Unknown unwrap type: " + unwrapType
        );
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private String resolveSchema(String tenantIdentifier) {

        if (tenantIdentifier == null) {
            return DEFAULT_SCHEMA;
        }

        String schema = tenantIdentifier
                .trim()
                .toLowerCase();

        if (schema.isBlank()) {
            return DEFAULT_SCHEMA;
        }

        if (!VALID_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tenant schema: " + schema
            );
        }

        return schema;
    }
}