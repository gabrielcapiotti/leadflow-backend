package com.leadflow.backend.security.jwt;

import com.leadflow.backend.entities.user.User;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMillis;
    private final String issuer;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expirationMillis,
            @Value("${security.jwt.issuer:leadflow}") String issuer
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
        this.issuer = issuer;
    }

    /* ======================================================
       TOKEN GENERATION
       ====================================================== */

    public String generateToken(User user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuer(issuer)
                .setId(UUID.randomUUID().toString()) // jti
                .setIssuedAt(Date.from(now))
                .setExpiration(
                        Date.from(now.plusMillis(expirationMillis))
                )
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().getName())
                .claim("tenant", user.getTenant().getSchemaName())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    public boolean isTokenValid(
            String token,
            UserDetails userDetails,
            UUID expectedUserId,
            String expectedTenant
    ) {

        try {

            final String email = extractEmail(token);
            final UUID tokenUserId = extractUserId(token);
            final String tokenTenant = extractTenant(token);

            return email.equals(userDetails.getUsername())
                    && tokenUserId.equals(expectedUserId)
                    && tokenTenant.equals(expectedTenant)
                    && !isTokenExpired(token);

        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /* ======================================================
       CLAIM EXTRACTION
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
        String value = extractClaim(
                token,
                claims -> claims.get("userId", String.class)
        );
        return UUID.fromString(value);
    }

    public String extractRole(String token) {
        return extractClaim(
                token,
                claims -> claims.get("role", String.class)
        );
    }

    public String extractTenant(String token) {
        return extractClaim(
                token,
                claims -> claims.get("tenant", String.class)
        );
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}