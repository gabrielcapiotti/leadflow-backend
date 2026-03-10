package com.leadflow.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Objects;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService =
                Objects.requireNonNull(rateLimitService, "RateLimitService must not be null");
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws IOException {

        HttpServletRequest safeRequest =
                Objects.requireNonNull(request, "HttpServletRequest must not be null");

        HttpServletResponse safeResponse =
                Objects.requireNonNull(response, "HttpServletResponse must not be null");

        Objects.requireNonNull(handler, "Handler must not be null");

        String scope = resolveScope(safeRequest);
        String key = resolveKey(safeRequest);

        boolean allowed = rateLimitService.tryConsume(key, scope);

        if (!allowed) {

            safeResponse.setStatus(429);
            safeResponse.setContentType("application/json");

            safeResponse.getWriter().write("""
                {
                    "error": "rate_limit_exceeded",
                    "message": "Too many requests"
                }
                """);

            return false;
        }

        return true;
    }

    /**
     * Determina qual escopo de rate limit aplicar
     */
    private String resolveScope(HttpServletRequest request) {

        String path = request.getRequestURI();

        if (path.startsWith("/ai")) {
            return "ai";
        }

        if (path.startsWith("/auth")) {
            return "auth";
        }

        if (path.startsWith("/admin")) {
            return "admin";
        }

        if (path.startsWith("/billing/webhook") || path.startsWith("/stripe/webhook")) {
            return "webhook";
        }

        return "global";
    }

    /**
     * Resolve a chave usada no rate limit.
     * Preferência:
     * 1) vendor header
     * 2) IP
     */
    private String resolveKey(HttpServletRequest request) {

        String vendorId = request.getHeader("X-VENDOR-ID");

        if (vendorId != null && !vendorId.isBlank()) {
            return vendorId;
        }

        return request.getRemoteAddr();
    }
}