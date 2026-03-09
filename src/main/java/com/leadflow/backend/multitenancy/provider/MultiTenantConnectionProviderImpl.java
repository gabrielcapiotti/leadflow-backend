package com.leadflow.backend.multitenancy.provider;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "multitenancy.enabled", havingValue = "true", matchIfMissing = true)
public class MultiTenantConnectionProviderImpl
        implements MultiTenantConnectionProvider<String> {

    private static final Logger log =
            LoggerFactory.getLogger(MultiTenantConnectionProviderImpl.class);

    private static final String DEFAULT_SCHEMA = "public";

    /**
     * Validação do nome do schema para evitar SQL injection.
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

        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /* ======================================================
       TENANT CONNECTION
       ====================================================== */

    @Override
    public Connection getConnection(String tenantIdentifier)
            throws SQLException {

        String schema = resolveSchema(tenantIdentifier);

        Connection connection = getAnyConnection();

        try {

            switchSchema(connection, schema);

        } catch (SQLException ex) {

            log.error("Failed switching schema to {}", schema, ex);

            try {
                resetSchema(connection);
            } catch (SQLException resetEx) {
                log.error("Failed resetting schema after error", resetEx);
            }

            throw ex;
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
            log.warn("Failed resetting schema on connection release", ex);
        }

        releaseAnyConnection(connection);
    }

    /* ======================================================
       SCHEMA MANAGEMENT
       ====================================================== */

    private void switchSchema(Connection connection, String schema)
            throws SQLException {

        String sql = "SET search_path TO \"" + schema + "\", public";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        log.debug("Connection switched to schema: {}", schema);
    }

    private void resetSchema(Connection connection)
            throws SQLException {

        String sql = "SET search_path TO " + DEFAULT_SCHEMA;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        log.debug("Connection schema reset to {}", DEFAULT_SCHEMA);
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

        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType)
                || MultiTenantConnectionProviderImpl.class.isAssignableFrom(unwrapType);
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