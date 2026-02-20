package com.leadflow.backend.multitenancy.config;

import lombok.RequiredArgsConstructor;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class MultiTenantHibernateConfig implements HibernatePropertiesCustomizer {

    private final MultiTenantConnectionProvider<String> multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {

        // Hibernate 6 → propriedade correta
        hibernateProperties.put(
                "hibernate.multiTenancy",
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