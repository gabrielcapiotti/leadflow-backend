package com.leadflow.backend.config;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base abstrata para testes de integração com PostgreSQL via Testcontainers.
 *
 * ✔ Lifecycle gerenciado pelo JUnit
 * ✔ Sem resource leak
 * ✔ Compatível com Spring Boot
 * ✔ Compatível com Flyway
 */
@Testcontainers
@ActiveProfiles("integration")
public abstract class AbstractPostgresContainerTest {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("postgres:16-alpine")
                    .asCompatibleSubstituteFor("postgres");

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withReuse(true); // melhora performance local

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES_CONTAINER::getDriverClassName);

        // Hibernate
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        // Flyway
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    }
}