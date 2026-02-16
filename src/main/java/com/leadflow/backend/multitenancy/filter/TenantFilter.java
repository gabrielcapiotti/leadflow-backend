package com.leadflow.backend.multitenancy.filter;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.resolver.JwtTenantResolver;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class TenantFilter implements Filter {

    private final JwtTenantResolver jwtTenantResolver;

    private static final String DEFAULT_TENANT = "public";

    public TenantFilter(JwtTenantResolver jwtTenantResolver) {
        this.jwtTenantResolver = jwtTenantResolver;
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        try {

            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String tenant = resolveTenantSafely(httpRequest);

            TenantContext.setTenant(
                    tenant != null ? tenant : DEFAULT_TENANT
            );

            chain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Resolve o tenant de forma segura, sem quebrar o fluxo
     * caso o token esteja ausente ou inválido.
     */
    private String resolveTenantSafely(HttpServletRequest request) {

        try {
            return jwtTenantResolver.resolveTenant(request);
        } catch (Exception ex) {
            // ⚠️ Nunca quebrar a requisição aqui.
            // A autenticação será tratada pelo JwtAuthenticationFilter.
            return null;
        }
    }
}
