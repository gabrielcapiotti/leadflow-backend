package com.leadflow.backend.multitenancy.identifier;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CurrentTenantIdentifierResolverImpl
        implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_TENANT = "public";

    /**
     * Permite apenas letras, números e underscore.
     * Evita SQL Injection via schema name.
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

    /**
     * Indica se o Hibernate deve validar sessões existentes
     * quando o tenant muda.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
