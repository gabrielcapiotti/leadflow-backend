package com.leadflow.backend.security.jwt;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.CustomUserDetails;
import com.leadflow.backend.service.auth.UserSessionService;
import com.leadflow.backend.util.LogSanitizer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserSessionService userSessionService;
    private final TenantService tenantService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            UserSessionService userSessionService,
            TenantService tenantService
    ) {
        this.jwtService =
                Objects.requireNonNull(jwtService, "JwtService must not be null");

        this.userDetailsService =
                Objects.requireNonNull(userDetailsService, "UserDetailsService must not be null");

        this.userSessionService =
                Objects.requireNonNull(userSessionService, "UserSessionService must not be null");

        this.tenantService =
                Objects.requireNonNull(tenantService, "TenantService must not be null");
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

        String token = extractToken(safeRequest);

        if (token == null ||
                SecurityContextHolder.getContext().getAuthentication() != null) {

            safeFilterChain.doFilter(safeRequest, safeResponse);
            return;
        }

        try {

            String email = jwtService.extractEmail(token);

            if (email == null) {
                safeFilterChain.doFilter(safeRequest, safeResponse);
                return;
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            if (!(userDetails instanceof CustomUserDetails customUser)) {
                safeFilterChain.doFilter(safeRequest, safeResponse);
                return;
            }

            UUID userId = customUser.getId();
            String tenant = TenantContext.getTenant();

            if (tenant == null || tenant.isBlank()) {
                safeFilterChain.doFilter(safeRequest, safeResponse);
                return;
            }

            boolean baseValid =
                    jwtService.isTokenValid(token, userDetails, userId, tenant);

            if (!baseValid ||
                    !isTokenStillValidAfterPasswordChange(token, customUser)) {

                safeFilterChain.doFilter(safeRequest, safeResponse);
                return;
            }

            String tokenId = jwtService.extractTokenId(token);
            UUID tenantId = tenantService.getTenantIdBySchema(tenant);

            userSessionService.processSessionActivity(
                    tokenId,
                    tenantId,
                    safeRequest.getRemoteAddr(),
                    safeRequest.getHeader("User-Agent")
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(safeRequest)
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception ex) {

                        logger.debug("JWT authentication failed: {}", LogSanitizer.sanitize(ex.getMessage()));

            safeResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        safeFilterChain.doFilter(safeRequest, safeResponse);
    }

    private boolean isTokenStillValidAfterPasswordChange(
            String token,
            CustomUserDetails userDetails
    ) {

        Date issuedAt = jwtService.extractIssuedAt(token);

        if (issuedAt == null) {
            return false;
        }

        LocalDateTime tokenIssuedAt = issuedAt.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LocalDateTime credentialsUpdatedAt =
                userDetails.getCredentialsUpdatedAt();

        if (credentialsUpdatedAt == null) {
            return true;
        }

        LocalDateTime normalizedCredentialsUpdatedAt =
                credentialsUpdatedAt.truncatedTo(ChronoUnit.SECONDS);

        return !tokenIssuedAt.isBefore(normalizedCredentialsUpdatedAt);
    }

    private String extractToken(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring(7);
    }
}