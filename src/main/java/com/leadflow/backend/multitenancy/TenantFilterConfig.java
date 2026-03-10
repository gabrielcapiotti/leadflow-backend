package com.leadflow.backend.multitenancy;

import com.leadflow.backend.multitenancy.filter.TenantFilter;
import com.leadflow.backend.multitenancy.resolver.TenantResolver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantFilterConfig {

    @Bean
    public TenantFilter tenantFilter(TenantResolver tenantResolver) {
        return new TenantFilter(tenantResolver);
    }
}