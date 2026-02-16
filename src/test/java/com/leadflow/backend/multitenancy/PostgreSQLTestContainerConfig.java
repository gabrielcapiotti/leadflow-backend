package com.leadflow.backend.multitenancy;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class para testes de integração com PostgreSQL via Testcontainers.
 *
 * ✔ Usa lifecycle gerenciado pelo JUnit
 * ✔ Integra automaticamente com Spring
 * ✔ Não precisa chamar start() manualmente
 * ✔ Evita múltiplas inicializações
 */
@Testcontainers
public abstract class PostgreSQLTestContainerConfig {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
