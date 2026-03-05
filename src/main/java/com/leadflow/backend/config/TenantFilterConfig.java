package com.leadflow.backend.config;

import com.leadflow.backend.multitenancy.filter.TenantFilter;
import com.leadflow.backend.multitenancy.resolver.TenantResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantFilterConfig {

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter(
            TenantResolver resolver
    ) {

        FilterRegistrationBean<TenantFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(new TenantFilter(resolver));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);

        return registration;
    }
}