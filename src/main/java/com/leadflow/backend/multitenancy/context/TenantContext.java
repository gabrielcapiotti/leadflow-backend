package com.leadflow.backend.multitenancy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public final class TenantContext {

    private static final Logger logger =
            LoggerFactory.getLogger(TenantContext.class);

    /**
     * Apenas lowercase, números e underscore.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    /**
     * ThreadLocal que mantém o tenant da requisição atual.
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

        String normalized = tenant.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(normalized).matches()) {

            logger.error("Invalid tenant identifier received: {}", tenant);

            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenant
            );
        }

        CURRENT_TENANT.set(normalized);

        if (logger.isDebugEnabled()) {
            logger.debug("Tenant set to {}", normalized);
        }
    }

    /* ======================================================
       GET STRICT (DOMAIN USE)
       ====================================================== */

    /**
     * Deve ser usado apenas quando o tenant é obrigatório
     * (ex: serviços de domínio).
     */
    public static String getTenant() {

        String tenant = CURRENT_TENANT.get();

        if (tenant == null) {
            throw new IllegalStateException(
                    "No tenant set in current thread"
            );
        }

        return tenant;
    }

    /* ======================================================
       GET OPTIONAL (INFRASTRUCTURE USE)
       ====================================================== */

    /**
     * Usado por infraestrutura (Hibernate resolver, logs, etc).
     * Não lança exceção se não existir tenant.
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
     * para evitar vazamento de tenant entre requests.
     */
    public static void clear() {

        CURRENT_TENANT.remove();

        if (logger.isDebugEnabled()) {
            logger.debug("Tenant context cleared");
        }
    }
}