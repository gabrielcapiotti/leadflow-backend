package com.leadflow.backend.multitenancy.hibernate;

import javax.sql.DataSource;
import java.sql.Connection;

public class SchemaMultiTenantConnectionProvider {

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection(String tenantIdentifier) {
        try {
            Connection connection = dataSource.getConnection();
            connection.createStatement().execute("SET search_path TO " + tenantIdentifier);
            return connection;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getAnyConnection() {
        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void releaseConnection(String tenantIdentifier, Connection connection) {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }

    public void releaseAnyConnection(Connection connection) {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }

    public boolean supportsAggressiveRelease() {
        return false;
    }
}