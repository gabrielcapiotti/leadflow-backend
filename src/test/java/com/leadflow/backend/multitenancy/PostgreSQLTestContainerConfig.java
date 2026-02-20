package com.leadflow.backend.multitenancy;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class para testes de integração com PostgreSQL via Testcontainers.
 *
 * ✔ Lifecycle gerenciado pelo JUnit
 * ✔ Integra automaticamente com Spring
 * ✔ Compatível com Flyway
 * ✔ Compatível com multi-tenant SCHEMA
 */
@Testcontainers
public abstract class PostgreSQLTestContainerConfig {

    private static final String IMAGE = "postgres:16-alpine";

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withReuse(true);

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {

        // ================= DATASOURCE =================
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        // ================= HIBERNATE =================
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        // Multi-tenant por SCHEMA (Hibernate 6)
        registry.add("spring.jpa.properties.hibernate.multiTenancy", () -> "SCHEMA");

        // ================= FLYWAY =================
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    }
}