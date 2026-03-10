package com.leadflow.backend.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

/**
 * Health indicator for database connectivity and performance.
 * 
 * Performs periodic health checks including:
 * - Connection availability
 * - Query performance
 * - Connection pool status
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Slf4j
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private DataSource dataSource;

    @Override
    public Health health() {
        if (dataSource == null) {
            return Health.unknown()
                    .withDetail("service", "Database")
                    .withDetail("message", "DataSource not configured")
                    .build();
        }

        try {
            // Test database connectivity
            long startTime = System.currentTimeMillis();
            testDatabaseConnection();
            long duration = System.currentTimeMillis() - startTime;

            // Treat > 500ms as slow
            if (duration > 500) {
                log.warn("Database health check slow: {}ms", duration);
                return Health.status("DEGRADED")
                        .withDetail("service", "Database")
                        .withDetail("message", "Database responding slowly")
                        .withDetail("response_time_ms", duration)
                        .build();
            }

            return Health.up()
                    .withDetail("service", "Database")
                    .withDetail("response_time_ms", duration)
                    .withDetail("pool_type", getPoolType())
                    .build();

        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("service", "Database")
                    .withDetail("error", "Connection failed: " + e.getMessage())
                    .withException(e)
                    .build();
        }
    }

    /**
     * Test database connectivity with a simple query.
     *
     * @throws Exception If connection fails
     */
    private void testDatabaseConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (connection == null) {
                throw new Exception("DataSource returned null connection");
            }

            // Verify connection is valid
            if (connection.isClosed()) {
                throw new Exception("Connection is closed");
            }

            // Execute simple query to verify execution
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new Exception("Query returned no results");
                    }
                }
            }
        }
    }

    /**
     * Get the type of connection pool being used.
     *
     * @return Pool type (HikariCP, Tomcat, etc.)
     */
    private String getPoolType() {
        if (dataSource == null) {
            return "unknown";
        }

        String className = dataSource.getClass().getSimpleName();
        if (className.contains("HikariDataSource")) {
            return "HikariCP";
        } else if (className.contains("TomcatJdbc")) {
            return "Tomcat JDBC";
        } else if (className.contains("BasicDataSource")) {
            return "DBCP";
        } else {
            return className;
        }
    }
}
