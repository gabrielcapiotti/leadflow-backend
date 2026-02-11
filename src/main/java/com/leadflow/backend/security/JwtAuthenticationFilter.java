package com.leadflow.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(
            TokenService tokenService,
            UserDetailsServiceImpl userDetailsService
    ) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        // 1️⃣ Header inexistente ou inválido → segue a cadeia
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            // 2️⃣ Token inválido → segue sem autenticar
            if (!tokenService.isValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3️⃣ Evita sobrescrever autenticação existente
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                String email = tokenService.getEmail(token);

                // Segurança extra
                if (email == null || email.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 4️⃣ Registra usuário autenticado
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception ex) {
            // Token inválido ou corrompido NÃO derruba a aplicação
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
