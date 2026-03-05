package com.leadflow.backend.security;

import com.leadflow.backend.security.jwt.JwtAuthenticationFilter;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.service.auth.UserSessionService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        name = "security.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SecurityWebConfig {

    /* =====================================================
       AUTHENTICATION MANAGER
       ===================================================== */

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /* =====================================================
       JWT FILTER (CONDICIONAL)
       ===================================================== */

    @Bean
    @ConditionalOnProperty(
            name = "security.jwt.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            UserSessionService userSessionService,
            TenantService tenantService
    ) {
        return new JwtAuthenticationFilter(
                jwtService,
                userDetailsService,
            userSessionService,
            tenantService
        );
    }

    /* =====================================================
       SECURITY FILTER CHAIN
       ===================================================== */

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ObjectProvider<JwtAuthenticationFilter> jwtFilterProvider,
            RateLimitFilter rateLimitFilter
    ) throws Exception {

        http
            .securityMatcher("/**")

            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())

            .cors(cors -> {})

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                )
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        response.sendError(HttpServletResponse.SC_FORBIDDEN)
                )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/billing/checkout").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/webhooks/**").permitAll()
                .anyRequest().authenticated()
            )

            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'")
                )
                .contentTypeOptions(contentType -> {})
                .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                .referrerPolicy(referrer ->
                    referrer.policy(
                        org.springframework.security.web.header.writers
                                .ReferrerPolicyHeaderWriter
                                .ReferrerPolicy.NO_REFERRER
                    )
                )
            );

        JwtAuthenticationFilter jwtFilter = jwtFilterProvider.getIfAvailable();

        if (jwtFilter != null) {
            http.addFilterBefore(
                    jwtFilter,
                    UsernamePasswordAuthenticationFilter.class
            );
        }

        http.addFilterAfter(
            rateLimitFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}