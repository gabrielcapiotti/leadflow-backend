package com.leadflow.backend;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para testes de integração com:
 * - PostgreSQL via Testcontainers
 * - Flyway
 * - Hibernate Multi-Tenancy (SCHEMA)
 *
 * ✔ Lifecycle gerenciado pelo JUnit
 * ✔ Sem start manual
 * ✔ Compatível com CI
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Use o profile 'test' para padronizar os testes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    @SuppressWarnings("resource") // lifecycle gerenciado pelo Testcontainers
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("test")
                    .withPassword("test");
                    // ⚠ Evite withReuse(true) em CI

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",
                postgres::getDriverClassName);

        /* ==============================
           HIBERNATE
           ============================== */

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        registry.add("spring.jpa.properties.hibernate.multiTenancy",
                () -> "SCHEMA");

        /* ==============================
           FLYWAY (schema public)
           ============================== */

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.schemas", () -> "public");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "true");

        /* ==============================
           SPRING TEST
           ============================== */

        registry.add("spring.test.database.replace", () -> "none");
    }

    @Autowired
    private MockMvc mockMvc;
}