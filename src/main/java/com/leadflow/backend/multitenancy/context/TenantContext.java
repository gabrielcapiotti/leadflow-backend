package com.leadflow.backend.multitenancy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Pattern;

public final class TenantContext {

    private static final Logger logger =
            LoggerFactory.getLogger(TenantContext.class);

    /**
     * Apenas lowercase, números e underscore.
     * Validação mínima — regras mais rígidas devem ficar na camada de domínio.
     */
    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    // ❗ SEM valor inicial
    private static final ThreadLocal<String> CURRENT_TENANT =
            new ThreadLocal<>();

    private TenantContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /* ======================================================
       SET
       ====================================================== */

    public static void setTenant(String tenant) {

        if (Objects.isNull(tenant) || tenant.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant identifier cannot be null or blank"
            );
        }

        String normalized = tenant
                .trim()
                .toLowerCase();

        if (!VALID_SCHEMA.matcher(normalized).matches()) {
            logger.error("Invalid tenant identifier: {}", tenant);
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenant
            );
        }

        CURRENT_TENANT.set(normalized);

        logger.debug("Tenant set to: {}", normalized);
    }

    /* ======================================================
       GET STRICT (DOMAIN USE)
       ====================================================== */

    /**
     * Usar apenas quando tenant é obrigatório.
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
     * Usado pelo CurrentTenantIdentifierResolver.
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
     * Deve ser chamado no finally do filtro.
     */
    public static void clear() {
        CURRENT_TENANT.remove(); // correto
        logger.debug("Tenant context cleared.");
    }
}