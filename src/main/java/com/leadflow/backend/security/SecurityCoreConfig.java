package com.leadflow.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityCoreConfig {

    /**
     * PasswordEncoder global da aplicação.
     *
     * - Disponível em qualquer contexto (WEB ou NON-WEB)
     * - Necessário para AuthService
     * - Independente de SecurityFilterChain
     *
     * BCrypt é o padrão recomendado para aplicações REST.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {

        // Strength padrão 10 (equilíbrio entre segurança e performance)
        int strength = 10;

        return new BCryptPasswordEncoder(strength);
    }
}