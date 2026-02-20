package com.leadflow.backend.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    /**
     * Segurança completamente liberada para testes.
     *
     * ✔ Desabilita CSRF
     * ✔ Permite todas as requisições
     * ✔ Stateless
     * ✔ Evita interferência de JWT e TenantFilter
     */
    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll()
            )

            .securityContext(security ->
                security.requireExplicitSave(false)
            )

            .requestCache(cache ->
                cache.disable()
            );

        return http.build();
    }

    /**
     * Encoder real para manter compatibilidade com serviços
     * que utilizam BCrypt.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}