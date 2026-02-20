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
 * ✔ Evita múltiplas inicializações
 * ✔ Compatível com Flyway
 */
@Testcontainers
public abstract class PostgreSQLTestContainerConfig {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withReuse(true); // melhora performance

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        // Hibernate
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        // Flyway
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        // Evita conflitos com multi-tenant durante testes
        registry.add("spring.jpa.properties.hibernate.multi_tenancy", () -> "NONE");
    }
}
