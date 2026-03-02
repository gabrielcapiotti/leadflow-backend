package com.leadflow.backend.multitenancy.identifier;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CurrentTenantIdentifierResolverImpl
        implements CurrentTenantIdentifierResolver<String> {

    private static final Logger log =
            LoggerFactory.getLogger(CurrentTenantIdentifierResolverImpl.class);

    private static final String DEFAULT_TENANT = "public";

    /**
     * PostgreSQL identifier limit = 63 characters.
     * Only lowercase letters, numbers and underscore allowed.
     */
    private static final int MAX_IDENTIFIER_LENGTH = 63;

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]{1," + MAX_IDENTIFIER_LENGTH + "}$");

    /* ======================================================
       RESOLVE CURRENT TENANT
       ====================================================== */

    @Override
    public String resolveCurrentTenantIdentifier() {

        String tenant = TenantContext.getTenant();

        if (tenant == null || tenant.isBlank()) {
            log.trace("No tenant found in context. Falling back to default schema: {}",
                    DEFAULT_TENANT);
            return DEFAULT_TENANT;
        }

        String normalized = tenant
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!VALID_SCHEMA.matcher(normalized).matches()) {
            log.error("Invalid tenant identifier detected: {}", normalized);
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + normalized
            );
        }

        log.trace("Resolved tenant identifier: {}", normalized);

        return normalized;
    }

    /* ======================================================
       HIBERNATE CONTRACT
       ====================================================== */

    /**
     * Must return true in web multi-tenant environments.
     * Ensures Hibernate validates tenant changes
     * when reusing sessions.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}