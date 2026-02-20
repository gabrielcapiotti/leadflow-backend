package com.leadflow.backend.multitenancy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Pattern;

public final class TenantContext {

    private static final Logger logger =
            LoggerFactory.getLogger(TenantContext.class);

    private static final String DEFAULT_SCHEMA = "public";

    private static final Pattern VALID_SCHEMA =
            Pattern.compile("^[a-z0-9_]+$");

    private static final ThreadLocal<String> CURRENT_TENANT =
            ThreadLocal.withInitial(() -> DEFAULT_SCHEMA);

    private TenantContext() {
        // Utility class
    }

    /* ======================================================
       SET
       ====================================================== */

    public static void setTenant(String tenant) {

        if (Objects.isNull(tenant) || tenant.isBlank()) {
            logger.debug("Null/blank tenant received. Using default schema.");
            CURRENT_TENANT.set(DEFAULT_SCHEMA);
            return;
        }

        String normalized = tenant.trim().toLowerCase();

        if (!VALID_SCHEMA.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tenant identifier: " + tenant
            );
        }

        CURRENT_TENANT.set(normalized);
    }

    /* ======================================================
       GET
       ====================================================== */

    public static String getTenant() {
        return CURRENT_TENANT.get();
    }

    /* ======================================================
       CLEAR
       ====================================================== */

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}