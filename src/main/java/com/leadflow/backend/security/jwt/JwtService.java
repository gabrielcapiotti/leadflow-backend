package com.leadflow.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.leadflow.backend.entities.user.User;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expirationMillis
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMillis = expirationMillis;
    }

    /* ==========================
       GERAÇÃO DE TOKEN
       ========================== */

    public String generateToken(
            Long userId,
            String email,
            String role
            // futuramente: String tenantId
    ) {

        return Jwts.builder()
                .setSubject(email)
                .setClaims(Map.of(
                        "userId", userId,
                        "role", role
                        // "tenantId", tenantId
                ))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ==========================
       VALIDAÇÃO DE TOKEN
       ========================== */

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /* ==========================
       EXTRAÇÃO DE CLAIMS
       ========================== */

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims ->
                claims.get("userId", Long.class)
        );
    }

    public String extractRole(String token) {
        return extractClaim(token, claims ->
                claims.get("role", String.class)
        );
    }

    public String extractTenant(String token) {
        return extractClaim(token, claims ->
                claims.get("tenantId", String.class)
        );
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Object generateToken(User any) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateToken'");
    }
}
