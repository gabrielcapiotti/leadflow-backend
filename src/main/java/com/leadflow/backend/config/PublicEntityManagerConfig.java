package com.leadflow.backend.config;

import jakarta.persistence.EntityManagerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.leadflow.backend.repository.tenant", // Repositórios do schema public
        entityManagerFactoryRef = "publicEntityManagerFactory",
        transactionManagerRef = "publicTransactionManager"
)
public class PublicEntityManagerConfig {

    private final DataSource dataSource;

    public PublicEntityManagerConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean(name = "publicEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean publicEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.default_schema", "public");

        return builder
                .dataSource(dataSource)
                .packages("com.leadflow.backend.entities") // Entidades que vivem no public
                .persistenceUnit("public")
                .properties(properties)
                .build();
    }

    @Bean(name = "publicTransactionManager")
    public PlatformTransactionManager publicTransactionManager(
            @Qualifier("publicEntityManagerFactory") EntityManagerFactory publicEntityManagerFactory
    ) {
        return new JpaTransactionManager(publicEntityManagerFactory);
    }
}