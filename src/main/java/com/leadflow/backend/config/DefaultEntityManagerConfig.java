package com.leadflow.backend.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.leadflow.backend.repository.tenant",
        entityManagerFactoryRef = "tenantEntityManagerFactory",
        transactionManagerRef = "tenantTransactionManager"
)
public class DefaultEntityManagerConfig {

    private final DataSource dataSource;
    private final MultiTenantConnectionProvider<String> multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;

    public DefaultEntityManagerConfig(
            DataSource dataSource,
            MultiTenantConnectionProvider<String> multiTenantConnectionProvider,
            CurrentTenantIdentifierResolver<String> tenantIdentifierResolver
    ) {
        this.dataSource = dataSource;
        this.multiTenantConnectionProvider = multiTenantConnectionProvider;
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    @Bean(name = "tenantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {

        Map<String, Object> properties = new HashMap<>();

        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.multiTenancy", "SCHEMA");
        properties.put("hibernate.multi_tenant_connection_provider", multiTenantConnectionProvider);
        properties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);

        return builder
                .dataSource(dataSource)
                .packages("com.leadflow.backend.entities")
                .persistenceUnit("tenant")
                .properties(properties)
                .build();
    }

    @Bean(name = "tenantTransactionManager")
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory")
            EntityManagerFactory tenantEntityManagerFactory
    ) {
        return new JpaTransactionManager(tenantEntityManagerFactory);
    }
}