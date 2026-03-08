package com.leadflow.backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
public class PublicEntityManagerConfig {

    private final DataSource dataSource;

    public PublicEntityManagerConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* ======================================================
       ENTITY MANAGER FACTORY (PUBLIC SCHEMA)
       ====================================================== */

    @Bean(name = "publicEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean publicEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {

        Map<String, Object> properties = new HashMap<>();

        /*
         * Flyway controla estrutura do banco
         */
        properties.put("hibernate.hbm2ddl.auto", "none");

        /*
         * Garante uso do schema public
         */
        properties.put("hibernate.default_schema", "public");

        return builder
                .dataSource(dataSource)
                .packages(
                        "com.leadflow.backend.entities"   // entidades globais (ex: Tenant)
                )
                .persistenceUnit("public")
                .properties(properties)
                .build();
    }

    /* ======================================================
       TRANSACTION MANAGER
       ====================================================== */

    @Bean(name = "publicTransactionManager")
    public PlatformTransactionManager publicTransactionManager(
            @Qualifier("publicEntityManagerFactory")
            LocalContainerEntityManagerFactoryBean factoryBean
    ) {

        EntityManagerFactory emf = Objects.requireNonNull(
                factoryBean.getObject(),
                "Public EntityManagerFactory must not be null"
        );

        return new JpaTransactionManager(emf);
    }
}