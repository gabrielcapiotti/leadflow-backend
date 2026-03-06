package com.leadflow.backend.multitenancy;

import com.leadflow.backend.multitenancy.filter.TenantFilter;
import com.leadflow.backend.multitenancy.resolver.TenantResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantFilterConfig {

    @Bean
    public TenantFilter tenantFilter(TenantResolver tenantResolver) {
        return new TenantFilter(tenantResolver);
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistrationBean(
            TenantFilter tenantFilter
    ) {

        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(tenantFilter);
        registration.setName("tenantFilter");
        registration.addUrlPatterns("/*");

        // Executa antes do Spring Security
        registration.setOrder(1);

        // Suporte para requests assíncronos
        registration.setAsyncSupported(true);

        return registration;
    }
}