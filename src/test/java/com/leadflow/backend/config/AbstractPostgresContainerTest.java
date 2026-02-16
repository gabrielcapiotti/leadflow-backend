package com.leadflow.backend.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("integration")
public abstract class AbstractPostgresContainerTest {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("postgres:16-alpine")
                    .asCompatibleSubstituteFor("postgres");

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withReuse(true); // importante para performance

    static {
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES_CONTAINER::getDriverClassName);

        // Flyway opcional (controle por profile)
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
