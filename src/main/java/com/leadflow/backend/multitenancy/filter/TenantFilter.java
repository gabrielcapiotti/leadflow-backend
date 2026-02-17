package com.leadflow.backend.multitenancy.filter;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.resolver.JwtTenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(
    name = "multitenancy.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class TenantFilter extends OncePerRequestFilter {

    private static final String DEFAULT_TENANT = "public";

    private final JwtTenantResolver jwtTenantResolver;

    public TenantFilter(JwtTenantResolver jwtTenantResolver) {
        this.jwtTenantResolver = jwtTenantResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String tenant = resolveTenantSafely(request);

            TenantContext.setTenant(
                    tenant != null ? tenant : DEFAULT_TENANT
            );

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Resolve o tenant de forma segura.
     * Nunca deve quebrar o fluxo da requisição.
     */
    private String resolveTenantSafely(HttpServletRequest request) {

        try {
            return jwtTenantResolver.resolveTenant(request);
        } catch (Exception ex) {
            return null;
        }
    }
}
