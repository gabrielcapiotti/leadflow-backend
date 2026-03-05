package com.leadflow.backend.multitenancy.hibernate;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import javax.sql.DataSource;
import java.sql.Connection;

public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection(String tenantIdentifier) {

        try {

            Connection connection = dataSource.getConnection();

            connection.createStatement()
                    .execute("SET search_path TO " + tenantIdentifier);

            return connection;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public Connection getAnyConnection() {

        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) {

        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void releaseAnyConnection(Connection connection) {

        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to release connection for tenant: " + tenantIdentifier, e);
        }
    }
}