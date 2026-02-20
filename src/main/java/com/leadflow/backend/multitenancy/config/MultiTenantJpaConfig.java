package com.leadflow.backend.multitenancy.config;

import com.leadflow.backend.multitenancy.identifier.CurrentTenantIdentifierResolverImpl;
import com.leadflow.backend.multitenancy.provider.SchemaMultiTenantConnectionProvider;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MultiTenantJpaConfig implements HibernatePropertiesCustomizer {

    private final SchemaMultiTenantConnectionProvider connectionProvider;
    private final CurrentTenantIdentifierResolverImpl tenantResolver;

    public MultiTenantJpaConfig(
            SchemaMultiTenantConnectionProvider connectionProvider,
            CurrentTenantIdentifierResolverImpl tenantResolver
    ) {
        this.connectionProvider = connectionProvider;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public void customize(Map<String, Object> properties) {

        // Hibernate 6.x exige String literal
        properties.put("hibernate.multiTenancy", "SCHEMA");

        // Provider responsável por aplicar SET search_path
        properties.put("hibernate.multi_tenant_connection_provider", connectionProvider);

        // Resolver que informa o tenant atual ao Hibernate
        properties.put("hibernate.tenant_identifier_resolver", tenantResolver);
    }
}