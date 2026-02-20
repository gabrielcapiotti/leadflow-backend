package com.leadflow.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("integration")
public abstract class IntegrationTestBase {

    private static final String IMAGE = "postgres:16-alpine";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withReuse(true); // ⚡ performance

    static {
        postgres.start(); // garante start único
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        // 🔒 Hibernate
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // 🔁 Desativa replace automático de DB em testes
        registry.add("spring.test.database.replace", () -> "none");

        // 🧠 Evita problema de multi-tenant com pool
        registry.add("spring.jpa.properties.hibernate.multi_tenancy", () -> "SCHEMA");
    }
}
