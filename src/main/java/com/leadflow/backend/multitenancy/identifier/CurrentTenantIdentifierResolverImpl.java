package com.leadflow.backend.multitenancy.identifier;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CurrentTenantIdentifierResolverImpl
        implements CurrentTenantIdentifierResolver<String> {

    private static final Logger log =
            LoggerFactory.getLogger(CurrentTenantIdentifierResolverImpl.class);

    /**
     * Schema base obrigatório.
     * Deve existir fisicamente no banco.
     */
    private static final String DEFAULT_TENANT = "public";

    /**
     * Regex estrita para evitar SQL injection via schema name.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    @Override
    public String resolveCurrentTenantIdentifier() {

        String tenant = TenantContext.getTenant();

        // 🔹 Fallback obrigatório (evita falha no bootstrap do Hibernate)
        if (tenant == null || tenant.isBlank()) {

            log.trace("No tenant found in context. Falling back to default schema: {}", DEFAULT_TENANT);

            return DEFAULT_TENANT;
        }

        String normalizedTenant = tenant.trim().toLowerCase();

        // 🔒 Defesa contra schema injection
        if (!VALID_SCHEMA.matcher(normalizedTenant).matches()) {

            log.error("Invalid tenant identifier detected: {}", normalizedTenant);

            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + normalizedTenant
            );
        }

        log.trace("Resolved tenant identifier: {}", normalizedTenant);

        return normalizedTenant;
    }

    /**
     * true = Hibernate valida o tenant ao reutilizar sessão.
     *
     * ESSENCIAL em ambiente web multi-tenant:
     * evita vazamento de schema entre requisições.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    /**
     * Hibernate 6 pode chamar este método durante bootstrap.
     * Mantemos consistente com DEFAULT_TENANT.
     */
    public String resolveTenantIdentifier(Object entity) {
        return resolveCurrentTenantIdentifier();
    }
}