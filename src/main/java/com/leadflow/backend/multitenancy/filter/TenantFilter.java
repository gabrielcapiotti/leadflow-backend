package com.leadflow.backend.multitenancy.filter;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.resolver.TenantResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;

    public TenantFilter(TenantResolver tenantResolver) {
        this.tenantResolver =
                Objects.requireNonNull(tenantResolver, "TenantResolver must not be null");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        String path = request.getRequestURI();

        return path.startsWith("/actuator")
                || path.startsWith("/health")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        HttpServletRequest safeRequest =
                Objects.requireNonNull(request, "HttpServletRequest must not be null");

        HttpServletResponse safeResponse =
                Objects.requireNonNull(response, "HttpServletResponse must not be null");

        FilterChain safeFilterChain =
                Objects.requireNonNull(filterChain, "FilterChain must not be null");

        // Always clear before processing to avoid stale context
        TenantContext.clear();
        
        String tenant = null;
        try {

            tenant = tenantResolver.resolveTenant(safeRequest);

            if (tenant == null || tenant.isBlank()) {

                safeResponse.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Header 'X-Tenant-ID' é obrigatório"
                );
                return;
            }

            TenantContext.setTenant(tenant);

            safeFilterChain.doFilter(safeRequest, safeResponse);

        } catch (IllegalArgumentException ex) {

            safeResponse.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    ex.getMessage()
            );

        } catch (Exception ex) {

            safeResponse.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Erro ao resolver tenant"
            );

        } finally {

            // Clean up ThreadLocal to prevent leaks between requests
            TenantContext.clear();
        }
    }
}