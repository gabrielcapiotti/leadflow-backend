package com.leadflow.backend.multitenancy.resolver;

import com.leadflow.backend.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

@Component
public class JwtTenantResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtTenantResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String resolveTenant(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Aqui NÃO precisamos validar novamente se já existe JwtAuthenticationFilter
            return jwtService.extractTenant(token);

        } catch (Exception ex) {
            return null;
        }
    }
}
