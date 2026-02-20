package com.leadflow.backend.multitenancy.filter;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(TenantFilter.class);

    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantService tenantService;

    public TenantFilter(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();

        // Permitir endpoints públicos
        return path.startsWith("/auth")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String tenantIdentifier = request.getHeader(TENANT_HEADER);

        try {

            if (tenantIdentifier == null || tenantIdentifier.isBlank()) {

                logger.warn("Missing tenant header on request: {} {}",
                        request.getMethod(),
                        request.getRequestURI()
                );

                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Missing X-Tenant-ID header"
                );
                return;
            }

            Optional<String> schemaOptional =
                    tenantService.resolveSchemaByTenantIdentifier(tenantIdentifier);

            if (schemaOptional.isEmpty()) {

                logger.warn("Invalid tenant identifier: {}", tenantIdentifier);

                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "Tenant not found"
                );
                return;
            }

            String schemaName = schemaOptional.get();

            tenantService.validateSchemaName(schemaName);

            TenantContext.setTenant(schemaName);

            logger.debug("Tenant resolved: {}", schemaName);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {

            logger.error("Tenant resolution failed", ex);

            if (!response.isCommitted()) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Tenant resolution error"
                );
            }

        } finally {

            TenantContext.clear();
        }
    }
}