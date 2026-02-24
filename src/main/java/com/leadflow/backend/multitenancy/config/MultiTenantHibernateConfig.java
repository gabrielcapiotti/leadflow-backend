package com.leadflow.backend.multitenancy.config;

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
public class MultiTenantHibernateConfig implements HibernatePropertiesCustomizer {

    private final MultiTenantConnectionProvider<String> multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> tenantIdentifierResolver;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {

        /*
         * Hibernate 6+
         * Estratégia SCHEMA é automaticamente ativada
         * quando MultiTenantConnectionProvider é definido.
         */

        hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
                multiTenantConnectionProvider
        );

        hibernateProperties.put(
                AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                tenantIdentifierResolver
        );
    }
}