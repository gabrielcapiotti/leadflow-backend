package com.leadflow.backend.security.jwt;

import com.leadflow.backend.entities.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expirationMillis
    ) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 characters"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expirationMillis = expirationMillis;
    }

    /* ======================================================
       GERAÇÃO DE TOKEN
       ====================================================== */

    public String generateToken(User user) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().getName());
        claims.put("tenant", user.getTenant().getSchemaName());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + expirationMillis)
                )
                .signWith(signingKey)
                .compact();
    }

    /* ======================================================
       VALIDAÇÃO
       ====================================================== */

    /**
     * Validação padrão baseada em email + expiração
     */
    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {

        final String email = extractEmail(token);

        return email.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    /**
     * Validação completa (email + userId + expiração)
     */
    public boolean isTokenValid(
            String token,
            UserDetails userDetails,
            UUID expectedUserId
    ) {

        final String email = extractEmail(token);
        final UUID tokenUserId = extractUserId(token);

        return email.equals(userDetails.getUsername())
                && tokenUserId.equals(expectedUserId)
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /* ======================================================
       EXTRAÇÃO DE CLAIMS
       ====================================================== */

    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {
        return resolver.apply(extractAllClaims(token));
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(
                extractClaim(token,
                        claims -> claims.get("userId", String.class)
                )
        );
    }

    public String extractRole(String token) {
        return extractClaim(token,
                claims -> claims.get("role", String.class)
        );
    }

    public String extractTenant(String token) {
        return extractClaim(token,
                claims -> claims.get("tenant", String.class)
        );
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}