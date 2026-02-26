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

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    private final DataSource dataSource;

    public MultiTenantConnectionProviderImpl(DataSource dataSource) {
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

        Connection connection = getAnyConnection();

        String schema = resolveSchema(tenantIdentifier);

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema);
            log.trace("Switched connection to schema: {}", schema);
        } catch (SQLException ex) {
            connection.close();
            throw new SQLException("Failed to switch schema to: " + schema, ex);
        }

        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + DEFAULT_SCHEMA);
        } catch (SQLException ex) {
            log.error("Failed to reset schema to default", ex);
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
                || MultiTenantConnectionProviderImpl.class.isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new IllegalArgumentException("Cannot unwrap to: " + unwrapType);
    }

    private String resolveSchema(String tenantIdentifier) {

        if (Objects.isNull(tenantIdentifier) || tenantIdentifier.isBlank()) {
            return DEFAULT_SCHEMA;
        }

        String normalized = tenantIdentifier.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid tenant identifier: " + normalized);
        }

        return normalized;
    }
}