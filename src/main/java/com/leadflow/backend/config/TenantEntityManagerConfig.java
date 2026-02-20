package com.leadflow.backend.config;

import com.leadflow.backend.multitenancy.identifier.CurrentTenantIdentifierResolverImpl;
import com.leadflow.backend.multitenancy.provider.SchemaMultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
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
        basePackages = "com.leadflow.backend.repository",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.leadflow\\.backend\\.repository\\.tenant\\..*"
        ),
        entityManagerFactoryRef = "tenantEntityManagerFactory",
        transactionManagerRef = "tenantTransactionManager"
)
public class TenantEntityManagerConfig {

    private final DataSource dataSource;
    private final SchemaMultiTenantConnectionProvider connectionProvider;
    private final CurrentTenantIdentifierResolverImpl tenantResolver;

    public TenantEntityManagerConfig(
            DataSource dataSource,
            SchemaMultiTenantConnectionProvider connectionProvider,
            CurrentTenantIdentifierResolverImpl tenantResolver
    ) {
        this.dataSource = dataSource;
        this.connectionProvider = connectionProvider;
        this.tenantResolver = tenantResolver;
    }

    @Bean(name = "tenantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {

        Map<String, Object> properties = new HashMap<>();

        properties.put("hibernate.multiTenancy", "SCHEMA");
        properties.put("hibernate.multi_tenant_connection_provider", connectionProvider);
        properties.put("hibernate.tenant_identifier_resolver", tenantResolver);

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