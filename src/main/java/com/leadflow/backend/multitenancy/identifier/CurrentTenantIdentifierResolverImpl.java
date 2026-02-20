package com.leadflow.backend.multitenancy.identifier;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.Objects;
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

        // Nenhum tenant definido → usar public (ex: auth, tenants, etc.)
        if (Objects.isNull(tenant) || tenant.isBlank()) {
            return DEFAULT_TENANT;
        }

        tenant = tenant.trim().toLowerCase();

        // Segurança contra schema injection
        if (!VALID_SCHEMA.matcher(tenant).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenant
            );
        }

        return tenant;
    }

    /**
     * false permite que o Hibernate reutilize a mesma Session
     * mesmo que o tenant mude no ThreadLocal.
     *
     * Isso é necessário para aplicações web com ThreadLocal.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}