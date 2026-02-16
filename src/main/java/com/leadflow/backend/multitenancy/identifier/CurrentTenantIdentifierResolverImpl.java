package com.leadflow.backend.multitenancy.identifier;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import java.util.regex.Pattern;

public class CurrentTenantIdentifierResolverImpl
        implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_TENANT = "public";

    /**
     * Permite apenas letras, números e underscore.
     * Evita SQL injection via schema.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    @Override
    public String resolveCurrentTenantIdentifier() {

        String tenant = TenantContext.getTenant();

        if (tenant == null || tenant.isBlank()) {
            return DEFAULT_TENANT;
        }

        tenant = tenant.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(tenant).matches()) {
            return DEFAULT_TENANT;
        }

        return tenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
