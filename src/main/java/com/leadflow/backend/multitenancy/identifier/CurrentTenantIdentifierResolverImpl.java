package com.leadflow.backend.multitenancy.identifier;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "multitenancy.enabled", havingValue = "true", matchIfMissing = true)
public class CurrentTenantIdentifierResolverImpl
        implements CurrentTenantIdentifierResolver<String> {

    private static final Logger log =
            LoggerFactory.getLogger(CurrentTenantIdentifierResolverImpl.class);

    /**
     * Schema padrão quando nenhum tenant está definido.
     */
    private static final String DEFAULT_TENANT = "public";

    /**
     * Regex para validar nome de schema PostgreSQL.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]{1,63}$");

    /* ======================================================
       RESOLVE CURRENT TENANT
       ====================================================== */

    @Override
    public String resolveCurrentTenantIdentifier() {

        String tenant = TenantContext.getIfPresent();

        if (Objects.isNull(tenant) || tenant.isBlank()) {

            log.debug(
                    "Tenant context empty. Using default schema: {}",
                    DEFAULT_TENANT
            );

            return DEFAULT_TENANT;
        }

        String normalized = tenant
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!VALID_SCHEMA.matcher(normalized).matches()) {

            log.error(
                    "Invalid tenant identifier detected: {}",
                    normalized
            );

            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + normalized
            );
        }

        log.debug("Resolved tenant identifier: {}", normalized);

        return normalized;
    }

    /* ======================================================
       HIBERNATE CONTRACT
       ====================================================== */

    /**
     * Permite que Hibernate valide o tenant atual
     * quando reutilizar sessões existentes.
     *
     * Isso é importante quando o TenantContext
     * muda entre requisições.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}