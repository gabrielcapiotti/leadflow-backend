package com.leadflow.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String GLOBAL_SCOPE = "global";
    private static final String AI_SCOPE = "ai";
    private static final String AUTH_SCOPE = "auth";
    private static final String ADMIN_SCOPE = "admin";
    private static final String WEBHOOK_SCOPE = "webhook";

    private final RateLimitService rateLimitService;
    private final boolean rateLimitEnabled;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            @Value("${security.rate-limit.enabled:true}") boolean rateLimitEnabled
    ) {
        this.rateLimitService = rateLimitService;
        this.rateLimitEnabled = rateLimitEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = extractRequestPath(request);
        String clientIp = resolveClientIp(request);

        boolean globalAllowed = rateLimitService.tryConsume(
                GLOBAL_SCOPE + ":" + clientIp,
                GLOBAL_SCOPE
        );

        if (!globalAllowed) {
            writeLimitExceeded(response);
            return;
        }

        String scope = resolveScope(requestPath);

        if (scope == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String principal = resolvePrincipalOrIp(request);
        String rateLimitKey = scope + ":" + principal;

        boolean allowed = rateLimitService.tryConsume(rateLimitKey, scope);

        if (!allowed) {
            writeLimitExceeded(response);
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

        if (requestPath.startsWith("/auth")) {
            return AUTH_SCOPE;
        }

        if (requestPath.startsWith("/admin")) {
            return ADMIN_SCOPE;
        }

        if (requestPath.startsWith("/webhooks")) {
            return WEBHOOK_SCOPE;
        }

        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
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

    private void writeLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Too many requests");
    }
}