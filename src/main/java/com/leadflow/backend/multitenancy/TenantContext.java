package com.leadflow.backend.multitenancy;

public final class TenantContext {

    private static final String DEFAULT_TENANT = "public";

    private static final ThreadLocal<String> CURRENT_TENANT =
            new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /* ======================================================
       SET TENANT
       ====================================================== */

    public static void setTenant(String tenant) {
        CURRENT_TENANT.set(tenant);
    }

    /* ======================================================
       GET TENANT (SAFE)
       ====================================================== */

    public static String getTenant() {
        String tenant = CURRENT_TENANT.get();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }

    /* ======================================================
       REQUIRE TENANT (STRICT MODE)
       ====================================================== */

    public static String requireTenant() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null) {
            throw new IllegalStateException("No tenant set in current thread");
        }
        return tenant;
    }

    /* ======================================================
       CHECK STATE
       ====================================================== */

    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /* ======================================================
       CLEAR
       ====================================================== */

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}