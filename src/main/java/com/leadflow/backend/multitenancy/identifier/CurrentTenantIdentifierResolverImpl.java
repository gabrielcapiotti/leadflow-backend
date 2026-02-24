package com.leadflow.backend.multitenancy.identifier;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class CurrentTenantIdentifierResolverImpl
        implements CurrentTenantIdentifierResolver<String> {

    private static final Logger logger =
            LoggerFactory.getLogger(CurrentTenantIdentifierResolverImpl.class);

    /**
     * Schema base obrigatório.
     * Deve existir fisicamente no banco.
     */
    private static final String DEFAULT_TENANT = "public";

    /**
     * Proteção contra schema injection.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    @Override
    public String resolveCurrentTenantIdentifier() {

        String tenant = TenantContext.getTenant();

        // 🔥 Fallback obrigatório para evitar quebra no bootstrap
        if (Objects.isNull(tenant) || tenant.isBlank()) {

            logger.trace("No tenant in context. Using default schema: {}", DEFAULT_TENANT);

            return DEFAULT_TENANT;
        }

        String normalizedTenant = tenant.trim().toLowerCase();

        // 🔐 Defesa contra injeção de schema
        if (!VALID_SCHEMA.matcher(normalizedTenant).matches()) {

            logger.error("Invalid tenant identifier detected: {}", normalizedTenant);

            // Não lançar exceção genérica em produção pode ser perigoso,
            // mas aqui é aceitável porque trata tentativa maliciosa.
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + normalizedTenant
            );
        }

        logger.trace("Resolved tenant identifier: {}", normalizedTenant);

        return normalizedTenant;
    }

    /**
     * true = Hibernate valida o tenant ao reutilizar sessão.
     *
     * Essencial para evitar vazamento de schema
     * entre requisições em ambiente web multi-tenant.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}