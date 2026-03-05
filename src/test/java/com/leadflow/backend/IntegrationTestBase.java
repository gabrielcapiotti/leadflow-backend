package com.leadflow.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    private static final String IMAGE = "postgres:16-alpine";

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

        static {
                postgres.start();
        }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        // Datasource
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");

        // JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.jpa.properties.hibernate.multiTenancy",
                () -> "SCHEMA");

        // Disable Flyway in tests
        registry.add("spring.flyway.enabled", () -> "false");

        // Multitenancy
        registry.add("multitenancy.enabled", () -> "true");

        // JWT Configuration (>= 32 chars mandatory)
        registry.add("jwt.secret",
                () -> "0123456789abcdef0123456789abcdef");

        registry.add("jwt.expiration",
                () -> "3600000"); // 1h

        registry.add("jwt.refresh-expiration",
                () -> "86400000"); // 24h
    }

    @Autowired
    protected MockMvc mockMvc;
}