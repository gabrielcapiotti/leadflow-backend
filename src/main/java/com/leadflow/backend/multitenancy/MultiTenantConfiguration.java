package com.leadflow.backend.multitenancy;

/**
 * Configuration for multi-tenant functionality.
 * 
 * Configures default behaviors for tenant isolation.
 * The TenantFilter is registered separately in SecurityWebConfig
 * to ensure proper ordering before JWT authentication.
 *
 * @author LeadFlow Team
 * @version 1.0
 */
public class MultiTenantConfiguration {
    // TenantFilter registration moved to SecurityWebConfig
    // to ensure proper filter ordering: TenantFilter -> JwtFilter -> Spring Security
}
