package com.leadflow.backend;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
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
        havingValue = "true",
        matchIfMissing = false
)
public class MultiTenantHibernateConfigBackend
        implements HibernatePropertiesCustomizer {

    private final MultiTenantConnectionProvider<String> multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;

    @Override
    public void customize(Map<String, Object> properties) {

        // Provider de conexão multi-tenant
        properties.put(
                AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
                multiTenantConnectionProvider
        );

        // Resolver de tenant atual
        properties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                tenantIdentifierResolver
        );

        // 🔥 Hibernate 6 — chave correta
        properties.put(
                "hibernate.multi_tenancy",
                "SCHEMA"
        );
    }
}