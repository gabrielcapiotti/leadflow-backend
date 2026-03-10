package com.leadflow.backend.multitenancy;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local context for managing multi-tenant isolation.
 * 
 * Stores the current tenant ID in thread-local storage, ensuring
 * that all database queries and operations are automatically scoped
 * to the current tenant.
 * 
 * Usage:
 * TenantContext.setCurrentTenant("tenant-123");
 * // ... perform operations ...
 * TenantContext.clear();
 * 
 * Or with try-with-resources:
 * try (TenantContextScope scope = TenantContext.withTenant("tenant-123")) {
 *     // ... operations automatically scoped to tenant-123 ...
 * }
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = ThreadLocal.withInitial(() -> null);

    /**
     * Set the current tenant for this thread.
     *
     * @param tenantId The tenant ID to set
     */
    public static void setCurrentTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or blank");
        }
        CURRENT_TENANT.set(tenantId);
        log.debug("Tenant context set: {}", tenantId);
    }

    /**
     * Get the current tenant for this thread.
     *
     * @return The current tenant ID
     * @throws IllegalStateException If no tenant is set
     */
    public static String getCurrentTenant() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set for current thread");
        }
        return tenantId;
    }

    /**
     * Get the current tenant, or a default if not set.
     *
     * @param defaultTenant The default tenant ID if not set
     * @return The current tenant ID or default
     */
    public static String getCurrentTenantOrDefault(String defaultTenant) {
        String tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : defaultTenant;
    }

    /**
     * Check if a tenant is currently set.
     *
     * @return true if tenant is set, false otherwise
     */
    public static boolean hasTenantContext() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Clear the tenant context for this thread.
     * Should be called at the end of request processing.
     */
    public static void clear() {
        String tenantId = CURRENT_TENANT.get();
        CURRENT_TENANT.remove();
        log.debug("Tenant context cleared: {}", tenantId);
    }

    /**
     * Create a scoped tenant context that automatically clears on close.
     * Use with try-with-resources for automatic cleanup.
     *
     * @param tenantId The tenant ID for the scope
     * @return A closeable scope
     */
    public static TenantContextScope withTenant(String tenantId) {
        return new TenantContextScope(tenantId);
    }

    /**
     * Auto-closeable scope for tenant context management.
     * Automatically sets and clears tenant context.
     */
    public static class TenantContextScope implements AutoCloseable {
        private final String tenantId;
        private final String previousTenant;

        public TenantContextScope(String tenantId) {
            this.tenantId = tenantId;
            this.previousTenant = CURRENT_TENANT.get();
            setCurrentTenant(tenantId);
        }

        @Override
        public void close() {
            if (previousTenant != null) {
                setCurrentTenant(previousTenant);
            } else {
                clear();
            }
        }
    }
}
