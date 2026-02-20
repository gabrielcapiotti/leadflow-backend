package com.leadflow.backend.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class MultiTenantHibernateConfig implements HibernatePropertiesCustomizer {

    private final MultiTenantConnectionProvider<String> multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {

        hibernateProperties.put(
                "hibernate.multi_tenancy",
                "SCHEMA"
        );

        hibernateProperties.put(
                "hibernate.multi_tenant_connection_provider",
                multiTenantConnectionProvider
        );

        hibernateProperties.put(
                "hibernate.tenant_identifier_resolver",
                tenantIdentifierResolver
        );
    }
}