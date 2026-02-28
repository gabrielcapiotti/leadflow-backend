package com.leadflow.backend.security.jwt;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.security.CustomUserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            try {

                String email = jwtService.extractEmail(token);

                if (email != null) {

                    UserDetails userDetails =
                            userDetailsService.loadUserByUsername(email);

                    if (!(userDetails instanceof CustomUserDetails customUser)) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UUID expectedUserId = customUser.getId();
                    String currentTenant = TenantContext.getTenant();

                    boolean baseValid = jwtService.isTokenValid(
                            token,
                            userDetails,
                            expectedUserId,
                            currentTenant
                    );

                    if (baseValid && isTokenStillValidAfterPasswordChange(token, customUser)) {

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authToken);
                    }
                }

            } catch (Exception ignored) {
                // Token inválido ou manipulado → não autentica
            }
        }

        filterChain.doFilter(request, response);
    }

    /* ======================================================
       JWT INVALIDATION LOGIC
       ====================================================== */

    private boolean isTokenStillValidAfterPasswordChange(
            String token,
            CustomUserDetails userDetails
    ) {

        Date issuedAt = jwtService.extractIssuedAt(token);

        if (issuedAt == null) {
            return false;
        }

        LocalDateTime tokenIssuedAt =
                issuedAt.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

        LocalDateTime credentialsUpdatedAt =
                userDetails.getCredentialsUpdatedAt();

        if (credentialsUpdatedAt == null) {
            return true;
        }

        return !tokenIssuedAt.isBefore(credentialsUpdatedAt);
    }

    /* ======================================================
       TOKEN EXTRACTION
       ====================================================== */

    private String extractToken(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring(7);
    }
}