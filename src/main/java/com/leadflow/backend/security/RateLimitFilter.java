package com.leadflow.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AI_SCOPE = "ai";
    private static final String DASHBOARD_SCOPE = "dashboard";
    private static final String LEADS_SCOPE = "leads";
    private static final String LOGIN_SCOPE = "login";

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = extractRequestPath(request);
        String scope = resolveScope(requestPath);

        if (scope == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String principal = resolvePrincipalOrIp(request);
        String rateLimitKey = scope + ":" + principal;

        boolean allowed = rateLimitService.tryConsume(rateLimitKey);

        if (!allowed) {
            response.setStatus(429); // HTTP 429 - Too Many Requests
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Limite de requisições excedido.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }

        return uri;
    }

    private String resolveScope(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return null;
        }

        if (requestPath.startsWith("/ai")) {
            return AI_SCOPE;
        }

        if (requestPath.equals("/dashboard") ||
            requestPath.startsWith("/vendors/dashboard")) {
            return DASHBOARD_SCOPE;
        }

        if (requestPath.startsWith("/leads") ||
            requestPath.startsWith("/vendor-leads")) {
            return LEADS_SCOPE;
        }

        if (requestPath.equals("/auth/login")) {
            return LOGIN_SCOPE;
        }

        return null;
    }

    private String resolvePrincipalOrIp(HttpServletRequest request) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
            authentication.isAuthenticated() &&
            !(authentication instanceof AnonymousAuthenticationToken) &&
            authentication.getName() != null &&
            !authentication.getName().isBlank()) {

            return authentication.getName();
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}