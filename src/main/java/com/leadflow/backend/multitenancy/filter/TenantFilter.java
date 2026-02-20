package com.leadflow.backend.multitenancy.filter;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private final TenantService tenantService;

    public TenantFilter(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String tenantId = request.getHeader("X-Tenant-ID");

        try {

            if (tenantId == null || tenantId.isBlank()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Tenant-ID header");
                return;
            }

            String schemaName = tenantService.resolveSchemaByTenantId(tenantId);

            if (schemaName == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid tenant");
                return;
            }

            TenantContext.setTenant(schemaName);

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}
