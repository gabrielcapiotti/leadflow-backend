package com.leadflow.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(ObjectProvider<RateLimiterService> rateLimiterServiceProvider) {
        this.rateLimiterService = rateLimiterServiceProvider.getIfAvailable();
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String vendorHeader = request.getHeader("X-VENDOR-ID");

        if (vendorHeader == null || vendorHeader.isBlank()) {
            return true;
        }

        if (rateLimiterService == null) {
            return true;
        }

        UUID vendorId;
        try {
            vendorId = UUID.fromString(vendorHeader);
        } catch (IllegalArgumentException ex) {
            response.setStatus(400);
            response.getWriter().write("Invalid X-VENDOR-ID header");
            return false;
        }

        boolean allowed = rateLimiterService.allowRequest(vendorId);

        if (!allowed) {

            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded");

            return false;
        }

        return true;
    }
}