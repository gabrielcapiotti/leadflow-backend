package com.leadflow.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {

        String scope = resolveScope(request);
        String key = resolveKey(request);

        boolean allowed = rateLimitService.tryConsume(key, scope);

        if (!allowed) {

            response.setStatus(429); // Código de status HTTP para Too Many Requests
            response.setContentType("application/json");

            response.getWriter().write("""
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

        if (path.startsWith("/billing/webhook")) {
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