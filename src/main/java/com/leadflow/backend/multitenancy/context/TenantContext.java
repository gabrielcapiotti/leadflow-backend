package com.leadflow.backend.multitenancy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TenantContext {

    private static final Logger log =
            LoggerFactory.getLogger(TenantContext.class);

    /**
     * Regex segura para schema PostgreSQL.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]{1,63}$");

    /**
     * ThreadLocal que mantém o tenant atual.
     */
    private static final ThreadLocal<String> CURRENT_TENANT =
            new ThreadLocal<>();

    private TenantContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /* ======================================================
       SET
       ====================================================== */

    public static void setTenant(String tenant) {

        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant identifier cannot be null or blank"
            );
        }

        String normalized = tenant
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!VALID_SCHEMA.matcher(normalized).matches()) {

            log.error("Invalid tenant identifier received: {}", tenant);

            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenant
            );
        }

        CURRENT_TENANT.set(normalized);

        if (log.isDebugEnabled()) {
            log.debug("Tenant context set: {}", normalized);
        }
    }

    /* ======================================================
       GET STRICT
       ====================================================== */

    /**
     * Usado quando o tenant é obrigatório
     * (serviços de domínio).
     */
    public static String getTenant() {

        String tenant = CURRENT_TENANT.get();

        if (tenant == null) {

            log.error("Attempt to access tenant context but none is set");

            throw new IllegalStateException(
                    "No tenant set in current thread"
            );
        }

        return tenant;
    }

    /* ======================================================
       GET OPTIONAL
       ====================================================== */

    /**
     * Usado por infraestrutura (Hibernate resolver).
     */
    public static String getIfPresent() {
        return CURRENT_TENANT.get();
    }

    /* ======================================================
       STATE
       ====================================================== */

    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /* ======================================================
       CLEAR
       ====================================================== */

    /**
     * Deve ser chamado no finally do TenantFilter
     * para evitar vazamento entre requisições.
     */
    public static void clear() {

        if (CURRENT_TENANT.get() != null && log.isDebugEnabled()) {
            log.debug("Clearing tenant context: {}", CURRENT_TENANT.get());
        }

        CURRENT_TENANT.remove();
    }
}