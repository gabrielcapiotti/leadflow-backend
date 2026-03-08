package com.leadflow.backend.multitenancy;

import com.leadflow.backend.multitenancy.filter.TenantFilter;
import com.leadflow.backend.multitenancy.resolver.TenantResolver;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Profile;

import java.util.EnumSet;

@Configuration
public class TenantFilterConfig {

    @Bean
    public TenantFilter tenantFilter(TenantResolver tenantResolver) {
        return new TenantFilter(tenantResolver);
    }

    /**
     * Registro explícito do filtro de tenant.
     * Executa antes da cadeia de filtros do Spring Security.
     */
    @Bean
    @Profile("!test") // evita interferência nos testes @WebMvcTest
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(
            TenantFilter tenantFilter
    ) {

        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(tenantFilter);
        registration.setName("tenantFilter");

        // aplica em todas as rotas
        registration.addUrlPatterns("/*");

        // executa antes do Spring Security
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        // suporte a async
        registration.setAsyncSupported(true);

        // tipos de dispatch suportados
        registration.setDispatcherTypes(
                EnumSet.of(
                        DispatcherType.REQUEST,
                        DispatcherType.ASYNC,
                        DispatcherType.ERROR
                )
        );

        return registration;
    }
}