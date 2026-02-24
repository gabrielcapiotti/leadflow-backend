package com.leadflow.backend.security.jwt;

import com.leadflow.backend.entities.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtService.class);

    private final Key signingKey;
    private final long expirationMillis;
    private final String issuer;
    private final Clock clock;

    /* ========================= CONSTRUCTOR (SPRING) ========================= */

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expirationMillis,
            @Value("${security.jwt.issuer:leadflow}") String issuer
    ) {
        this(secret, expirationMillis, issuer, Clock.systemUTC());
    }

    /* ========================= CONSTRUCTOR (TEST) ========================= */

    public JwtService(
            String secret,
            long expirationMillis,
            String issuer,
            Clock clock
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
        this.clock = clock;
    }

    /* ========================= TOKEN GENERATION ========================= */

    public String generateToken(User user, String tenantSchema) {

        if (user == null || user.getId() == null || user.getRole() == null) {
            throw new IllegalStateException("Invalid user for token generation");
        }

        if (tenantSchema == null || tenantSchema.isBlank()) {
            throw new IllegalArgumentException("Tenant schema cannot be blank");
        }

        Instant now = Instant.now(clock);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getEmail())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMillis)))
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().getName())
                .claim("tenant", tenantSchema.trim().toLowerCase())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ========================= BASIC VALIDATION ========================= */

    public boolean isValid(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            extractAllClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            logger.warn("JWT expired");

        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT");
        }

        return false;
    }

    /* ========================= CONTEXT VALIDATION ========================= */

    public boolean isTokenValid(
            String token,
            UserDetails userDetails,
            UUID expectedUserId,
            String expectedTenant
    ) {

        if (!isValid(token)) {
            return false;
        }

        try {

            String email = extractEmail(token);
            UUID tokenUserId = extractUserId(token);
            String tokenTenant = extractTenant(token);

            return email.equals(userDetails.getUsername())
                    && tokenUserId.equals(expectedUserId)
                    && tokenTenant.equalsIgnoreCase(expectedTenant)
                    && !isTokenExpired(token);

        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token)
                .before(Date.from(Instant.now(clock)));
    }

    /* ========================= CLAIM EXTRACTION ========================= */

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

        if (value == null) {
            throw new IllegalArgumentException("userId claim missing");
        }

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

    /* ========================= INTERNAL PARSE ========================= */

    private Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(issuer)
                .setAllowedClockSkewSeconds(30)
                .setClock(() -> Date.from(Instant.now(clock)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}