package com.leadflow.backend.multitenancy.provider;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

@Component
public class SchemaMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String>, Serializable {

    private static final long serialVersionUID = 1L;

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

        try {
            connection.setSchema(schema);
        } catch (SQLException ex) {
            connection.setSchema(DEFAULT_SCHEMA);
        }

        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection)
            throws SQLException {

        try {
            connection.setSchema(DEFAULT_SCHEMA);
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
        return unwrapType.isAssignableFrom(getClass()) ||
               unwrapType.isAssignableFrom(MultiTenantConnectionProvider.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        return null;
    }

    /**
     * Sanitiza e valida schema antes de aplicar.
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
