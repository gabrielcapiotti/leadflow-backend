package com.leadflow.backend.multitenancy.config;

import com.leadflow.backend.multitenancy.identifier.CurrentTenantIdentifierResolverImpl;
import com.leadflow.backend.multitenancy.provider.SchemaMultiTenantConnectionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class MultiTenantJpaConfig {

    @Bean
    public SchemaMultiTenantConnectionProvider multiTenantConnectionProvider(
            DataSource dataSource) {
        return new SchemaMultiTenantConnectionProvider(dataSource);
    }

    @Bean
    public CurrentTenantIdentifierResolverImpl tenantIdentifierResolver() {
        return new CurrentTenantIdentifierResolverImpl();
    }
}