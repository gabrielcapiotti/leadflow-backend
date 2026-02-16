package com.leadflow.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {

        http
            // API REST → stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // CSRF desativado para testes com MockMvc
            .csrf(csrf -> csrf.disable())

            // Configuração realista de autorização
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register", "/auth/login").permitAll()
                .anyRequest().authenticated()
            )

            // Permite usar @WithMockUser
            .httpBasic(Customizer.withDefaults())

            // Não usa formulário de login
            .formLogin(form -> form.disable())

            // Mantém tratamento padrão de exceções de auth
            .exceptionHandling(Customizer.withDefaults());

        return http.build();
    }
}
