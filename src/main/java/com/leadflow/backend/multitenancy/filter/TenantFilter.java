package com.leadflow.backend.multitenancy.filter;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
        name = "multitenancy.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(TenantFilter.class);

    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantService tenantService;

    public TenantFilter(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /* ======================================================
       SKIP PATHS & DISPATCH TYPES
       ====================================================== */

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        DispatcherType dispatcherType = request.getDispatcherType();

        if (dispatcherType == DispatcherType.ASYNC ||
            dispatcherType == DispatcherType.ERROR) {
            return true;
        }

        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }

        return path.startsWith("/auth")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }

    /* ======================================================
       FILTER EXECUTION
       ====================================================== */

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String tenantIdentifier = request.getHeader(TENANT_HEADER);

        if (tenantIdentifier == null || tenantIdentifier.isBlank()) {

            logger.warn("Missing {} header: {} {}",
                    TENANT_HEADER,
                    request.getMethod(),
                    request.getRequestURI()
            );

            if (!response.isCommitted()) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Missing " + TENANT_HEADER + " header"
                );
            }
            return;
        }

        String normalizedTenant = tenantIdentifier.trim();

        try {

            Optional<String> schemaOptional =
                    tenantService.resolveSchemaByTenantIdentifier(normalizedTenant);

            if (schemaOptional.isEmpty()) {

                logger.warn("Invalid tenant identifier: {}", normalizedTenant);

                if (!response.isCommitted()) {
                    response.sendError(
                            HttpServletResponse.SC_NOT_FOUND,
                            "Tenant not found"
                    );
                }
                return;
            }

            String schemaName = schemaOptional.get();

            // Segurança contra schema injection
            tenantService.validateSchemaName(schemaName);

            TenantContext.setTenant(schemaName);

            logger.debug("Tenant resolved to schema: {}", schemaName);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {

            logger.error("Tenant resolution failed for identifier {}",
                    normalizedTenant, ex);

            if (!response.isCommitted()) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Tenant resolution error"
                );
            }

        } finally {
            // Garantia absoluta de limpeza do ThreadLocal
            TenantContext.clear();
        }
    }
}