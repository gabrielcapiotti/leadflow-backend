package com.leadflow.backend.security;

import com.leadflow.backend.security.jwt.JwtService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("!test")
@EnableMethodSecurity
public class SecurityConfig {

    /* ======================================================
       BEANS
       ====================================================== */

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    /* ======================================================
       FILTER CHAIN
       ====================================================== */

    @Bean
    @ConditionalOnMissingBean(name = "filterChain")
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        http

            // 🔒 REST API → CSRF desabilitado
            .csrf(csrf -> csrf.disable())

            // 🔐 Stateless API
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 🌍 CORS habilitado (importante para frontend)
            .cors(cors -> {})

            // 🔑 Authorization
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )

            // 🛡 Headers básicos
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'")
                )
            )

            // 🔐 JWT Authentication FIRST
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}