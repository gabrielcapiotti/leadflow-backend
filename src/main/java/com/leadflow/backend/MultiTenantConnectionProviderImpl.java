package com.leadflow.backend;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
public class MultiTenantConnectionProviderImpl
        implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

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

        try (Statement statement = connection.createStatement()) {

            // 🔥 ESSENCIAL
            statement.execute(
                    "SET search_path TO " + tenantIdentifier + ", public"
            );
        }

        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier,
                                  Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO public");
        }

        releaseAnyConnection(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}