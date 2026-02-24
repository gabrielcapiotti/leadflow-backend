package com.leadflow.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para testes de integração:
 * - PostgreSQL via Testcontainers
 * - Flyway
 * - Multi-tenancy por schema
 *
 * ✔ Lifecycle gerenciado pelo JUnit
 * ✔ Compatível com CI
 * ✔ Sem resource leak real
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration")
public abstract class IntegrationTestBase {

    private static final String IMAGE = "postgres:16-alpine";

    @Container
    @SuppressWarnings("resource") // lifecycle gerenciado pelo Testcontainers
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres");
                    // ⚠ Evite withReuse(true) em CI

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");

        /* ==============================
           HIBERNATE
           ============================== */

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");

        // ⚠ Multi-tenancy é configurado via HibernatePropertiesCustomizer
        // NÃO configure provider/resolver aqui.

        /* ==============================
           FLYWAY
           ============================== */

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.schemas", () -> "public");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");

        /* ==============================
           SPRING TEST
           ============================== */

        registry.add("spring.test.database.replace", () -> "none");
    }
}