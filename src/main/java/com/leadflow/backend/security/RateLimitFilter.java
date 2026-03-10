package com.leadflow.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

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
        this.rateLimitService =
                Objects.requireNonNull(rateLimitService, "RateLimitService must not be null");

        this.rateLimitEnabled = rateLimitEnabled;
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

        if (!rateLimitEnabled) {
            safeFilterChain.doFilter(safeRequest, safeResponse);
            return;
        }

        String requestPath = extractRequestPath(safeRequest);
        String clientIp = resolveClientIp(safeRequest);

        boolean globalAllowed = rateLimitService.tryConsume(clientIp, GLOBAL_SCOPE);

        if (!globalAllowed) {
            writeLimitExceeded(safeResponse);
            return;
        }

        String scope = resolveScope(requestPath);

        if (scope == null) {
            safeFilterChain.doFilter(safeRequest, safeResponse);
            return;
        }

        String principal = resolvePrincipalOrIp(safeRequest);

        boolean allowed = rateLimitService.tryConsume(principal, scope);

        if (!allowed) {
            writeLimitExceeded(safeResponse);
            return;
        }

        safeFilterChain.doFilter(safeRequest, safeResponse);
    }

    private String extractRequestPath(HttpServletRequest request) {

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null &&
            !contextPath.isBlank() &&
            uri.startsWith(contextPath)) {

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

        if (requestPath.startsWith("/billing/webhook") ||
            requestPath.startsWith("/stripe/webhook")) {
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
            !(authentication instanceof AnonymousAuthenticationToken)) {

            String principal = authentication.getName();

            if (principal != null && !principal.isBlank()) {
                return principal;
            }
        }

        return resolveClientIp(request);
    }

    private void writeLimitExceeded(HttpServletResponse response) throws IOException {

        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write("""
        {
          "error": "rate_limit_exceeded",
          "message": "Too many requests"
        }
        """);
    }
}