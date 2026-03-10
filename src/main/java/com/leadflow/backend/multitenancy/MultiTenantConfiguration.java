package com.leadflow.backend.multitenancy;

import com.leadflow.backend.multitenancy.filter.TenantFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for multi-tenant functionality.
 * 
 * Registers tenant context filter and configures default behaviors
 * for tenant isolation.
 *
 * @author LeadFlow Team
 * @version 1.0
 */
@Slf4j
@Configuration
public class MultiTenantConfiguration {

    /**
     * Register the tenant filter in the filter chain.
     * This filter runs before authentication/authorization filters
     * to properly set tenant context.
     *
     * @param tenantFilter The tenant filter bean
     * @return Filter registration with lowest order (runs first)
     */
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter tenantFilter) {
        FilterRegistrationBean<TenantFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(tenantFilter);
        registrationBean.setOrder(-1);  // Run before Spring Security filters
        registrationBean.addUrlPatterns("/api/*", "/auth/*", "/admin/*", "/stripe/*", "/webhook/*");
        log.info("TenantFilter registered in filter chain with order -1");
        return registrationBean;
    }
}
