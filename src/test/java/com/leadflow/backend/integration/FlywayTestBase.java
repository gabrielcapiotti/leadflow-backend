package com.leadflow.backend.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import com.leadflow.backend.exception.GlobalExceptionHandler;

@WebMvcTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Use o profile 'test' para padronizar os testes
@Import(GlobalExceptionHandler.class)
public abstract class FlywayTestBase {

    private static final String IMAGE = "postgres:16-alpine";

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

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");

        /* ==============================
           HIBERNATE
           ============================== */

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.multiTenancy", () -> "SCHEMA");

        /* ==============================
           FLYWAY ATIVADO
           ============================== */

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");

        registry.add("spring.test.database.replace", () -> "none");
        registry.add("multitenancy.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;
}