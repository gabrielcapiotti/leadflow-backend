package com.leadflow.backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
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

        // ⚠️ Flyway controla schema
        properties.put("hibernate.hbm2ddl.auto", "none");

        // 🔒 Garante que sempre use o schema public
        properties.put("hibernate.default_schema", "public");

        return builder
                .dataSource(dataSource)
                .packages("com.leadflow.backend.repository.public") // SOMENTE entidades globais
                .persistenceUnit("public")
                .properties(properties)
                .build();
    }

    @Bean(name = "publicTransactionManager")
    public PlatformTransactionManager publicTransactionManager(
            @Qualifier("publicEntityManagerFactory")
            EntityManagerFactory publicEntityManagerFactory
    ) {
        return new JpaTransactionManager(publicEntityManagerFactory);
    }
}