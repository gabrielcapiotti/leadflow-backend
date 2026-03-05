package com.leadflow.backend.security.jwt;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.CustomUserDetails;
import com.leadflow.backend.service.auth.UserSessionService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

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
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userSessionService = userSessionService;
        this.tenantService = tenantService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(token);

            if (email == null) {
                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!(userDetails instanceof CustomUserDetails customUser)) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = customUser.getId();
            String tenant = TenantContext.getTenant();

            if (tenant == null || tenant.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            boolean baseValid = jwtService.isTokenValid(
                    token,
                    userDetails,
                    userId,
                    tenant
            );

            if (!baseValid || !isTokenStillValidAfterPasswordChange(token, customUser)) {
                filterChain.doFilter(request, response);
                return;
            }

            String tokenId = jwtService.extractTokenId(token);
            UUID tenantId = tenantService.getTenantIdBySchema(tenant);

            // Manage session activity and log it
            userSessionService.processSessionActivity(
                    tokenId,
                    tenantId,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );

            // Authenticate user and set the security context
            UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception ex) {
            // Log detailed information about authentication failures
            logger.debug("JWT authentication failed: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Set HTTP status 401 on failure
        }

        filterChain.doFilter(request, response);
    }

    // Check if the token is still valid after password change
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

        LocalDateTime credentialsUpdatedAt = userDetails.getCredentialsUpdatedAt();

        if (credentialsUpdatedAt == null) {
            return true;
        }

        LocalDateTime normalizedCredentialsUpdatedAt =
                credentialsUpdatedAt.truncatedTo(ChronoUnit.SECONDS);

        // Ensure the token was issued before the password was changed
        return !tokenIssuedAt.isBefore(normalizedCredentialsUpdatedAt);
    }

    // Extract JWT token from the request header
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring(7); // Extract the token part from the header
    }
}