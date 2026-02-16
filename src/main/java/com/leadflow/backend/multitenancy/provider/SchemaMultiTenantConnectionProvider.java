package com.leadflow.backend.multitenancy.provider;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

public class SchemaMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String> {

    private static final String DEFAULT_SCHEMA = "public";

    /**
     * Permite apenas letras, números e underscore.
     * Defesa extra contra SQL injection.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        String schema = resolveSchemaSafely(tenantIdentifier);
        Connection connection = getAnyConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO \"" + schema + "\"");
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO \"" + DEFAULT_SCHEMA + "\"");
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
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    /**
     * Sanitiza e valida schema antes de aplicar no search_path.
     */
    private String resolveSchemaSafely(String tenantIdentifier) {

        if (tenantIdentifier == null || tenantIdentifier.isBlank()) {
            return DEFAULT_SCHEMA;
        }

        String schema = tenantIdentifier.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(schema).matches()) {
            return DEFAULT_SCHEMA;
        }

        return schema;
    }
}
