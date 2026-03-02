package com.leadflow.backend.multitenancy;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TenantContext {

    /**
     * Deve ser consistente com a validação da entidade Tenant:
     * - Começa com letra
     * - 3 a 50 caracteres
     * - Apenas lowercase, números e underscore
     */
    private static final Pattern VALID_TENANT =
            Pattern.compile("^[a-z][a-z0-9_]{2,49}$");

    private static final ThreadLocal<String> CURRENT_TENANT =
            new ThreadLocal<>();

    private TenantContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /* ======================================================
       SET TENANT
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

        if (!VALID_TENANT.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenant
            );
        }

        CURRENT_TENANT.set(normalized);
    }

    /* ======================================================
       GET TENANT (STRICT)
       ====================================================== */

    /**
     * Returns the current tenant.
     * Throws if not set.
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

    /**
     * Alias explícito para código de domínio.
     */
    public static String requireTenant() {
        return getTenant();
    }

    /* ======================================================
       OPTIONAL ACCESS (INFRASTRUCTURE USE ONLY)
       ====================================================== */

    /**
     * Returns tenant or null if not present.
     * Must only be used in infrastructure layers
     * (e.g. CurrentTenantIdentifierResolver).
     */
    public static String getIfPresent() {
        return CURRENT_TENANT.get();
    }

    /* ======================================================
       STATE CHECK
       ====================================================== */

    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /* ======================================================
       CLEAR CONTEXT
       ====================================================== */

    /**
     * MUST be called at the end of every request.
     * Prevents tenant leakage across threads.
     */
    public static void clear() {
        CURRENT_TENANT.remove(); // correto — nunca usar set(null)
    }
}