package com.leadflow.backend.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@ConditionalOnProperty(name = "multitenancy.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultEntityManagerConfig {

    private final DataSource dataSource;
    private final MultiTenantConnectionProvider<String> multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;

    public DefaultEntityManagerConfig(
            DataSource dataSource,
            MultiTenantConnectionProvider<String> multiTenantConnectionProvider,
            CurrentTenantIdentifierResolver<String> tenantIdentifierResolver
    ) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.multiTenantConnectionProvider = Objects.requireNonNull(multiTenantConnectionProvider);
        this.tenantIdentifierResolver = Objects.requireNonNull(tenantIdentifierResolver);
    }

    /* ======================================================
       ENTITY MANAGER FACTORY
       ====================================================== */

    @Bean(name = "tenantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {

        Map<String, Object> properties = new HashMap<>();

        properties.put("hibernate.multi_tenancy", "SCHEMA");
        properties.put("hibernate.multi_tenant_connection_provider", multiTenantConnectionProvider);
        properties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);

        return builder
                .dataSource(dataSource)
                .packages(
                        "com.leadflow.backend.entities",
                        "com.leadflow.domain.auth"
                )
                .persistenceUnit("tenant")
                .properties(properties)
                .build();
    }

    /*
     * Bean principal usado pelo Spring Boot
     */
    @Bean(name = "entityManagerFactory")
    @Primary
    public EntityManagerFactory entityManagerFactory(
            @Qualifier("tenantEntityManagerFactory")
            LocalContainerEntityManagerFactoryBean tenantFactory
    ) {

        EntityManagerFactory emf = tenantFactory.getObject();

        if (emf == null) {
            throw new IllegalStateException("EntityManagerFactory must not be null");
        }

        return emf;
    }

    /* ======================================================
       TRANSACTION MANAGER
       ====================================================== */

    @Bean(name = "tenantTransactionManager")
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("entityManagerFactory")
            EntityManagerFactory entityManagerFactory
    ) {

        Objects.requireNonNull(entityManagerFactory);

        return new JpaTransactionManager(entityManagerFactory);
    }

    /*
     * Transaction manager padrão do sistema
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory")
            EntityManagerFactory entityManagerFactory
    ) {

        Objects.requireNonNull(entityManagerFactory);

        return new JpaTransactionManager(entityManagerFactory);
    }

    /* ======================================================
       CLOCK (TESTABILITY)
       ====================================================== */

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}