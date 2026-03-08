package com.leadflow.backend.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_TENANT = "tenant";
    public static final String MDC_PATH = "path";
    public static final String MDC_METHOD = "method";
    public static final String MDC_IP = "ip";

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

        String correlationId = resolveCorrelationId(safeRequest);

        try {

            enrichMdc(safeRequest, correlationId);

            safeResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            safeFilterChain.doFilter(safeRequest, safeResponse);

        } finally {
            MDC.clear(); // evita vazamento entre threads
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {

        return Optional.ofNullable(request.getHeader(CORRELATION_ID_HEADER))
                .filter(id -> !id.isBlank())
                .orElse(UUID.randomUUID().toString());
    }

    private void enrichMdc(HttpServletRequest request, String correlationId) {

        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_PATH, request.getRequestURI());
        MDC.put(MDC_IP, resolveClientIp(request));

        // integração futura com multi-tenant
        // MDC.put(MDC_TENANT, TenantContext.getTenant());
    }

    private String resolveClientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}