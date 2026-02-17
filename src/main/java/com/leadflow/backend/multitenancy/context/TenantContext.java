package com.leadflow.backend.multitenancy.context;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private static final String DEFAULT_TENANT = "public";

    private TenantContext() {
        // Utility class
    }

    public static void setTenant(String tenant) {
        CURRENT_TENANT.set(tenant);
    }

    public static String getTenant() {
        String tenant = CURRENT_TENANT.get();
        return (tenant != null) ? tenant : DEFAULT_TENANT;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
