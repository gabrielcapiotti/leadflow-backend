package com.leadflow.backend.multitenancy.identifier;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CurrentTenantIdentifierResolverImpl
        implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_TENANT = "public";

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    @Override
    public String resolveCurrentTenantIdentifier() {

        String tenant = TenantContext.getTenant();

        if (tenant == null) {
            return DEFAULT_TENANT;
        }

        tenant = tenant.trim().toLowerCase();

        if (tenant.isBlank()) {
            return DEFAULT_TENANT;
        }

        if (!VALID_SCHEMA.matcher(tenant).matches()) {
            return DEFAULT_TENANT;
        }

        return tenant;
    }

    /**
     * Retornando false permite troca de tenant
     * dentro da mesma transação/session.
     *
     * Isso é essencial para testes e para
     * aplicações que usam ThreadLocal.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}