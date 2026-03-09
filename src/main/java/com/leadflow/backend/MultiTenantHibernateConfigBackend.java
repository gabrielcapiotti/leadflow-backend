package com.leadflow.backend;

import lombok.RequiredArgsConstructor;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "multitenancy.enabled",
        havingValue = "true"
)
public class MultiTenantHibernateConfigBackend
        implements HibernatePropertiesCustomizer {

    private final MultiTenantConnectionProvider<String> multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;

    @Override
    public void customize(Map<String, Object> properties) {

        /* ======================================================
           MULTITENANCY STRATEGY
           ====================================================== */

        properties.put(
                "hibernate.multiTenancy",
                "SCHEMA"
        );

        /* ======================================================
           CONNECTION PROVIDER
           ====================================================== */

        properties.put(
                "hibernate.multi_tenant_connection_provider",
                multiTenantConnectionProvider
        );

        /* ======================================================
           TENANT IDENTIFIER RESOLVER
           ====================================================== */

        properties.put(
                "hibernate.multi_tenant_identifier_resolver",
                tenantIdentifierResolver
        );
    }
}