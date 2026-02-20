package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.User;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {

    private static final Logger logger =
            LoggerFactory.getLogger(TokenService.class);

    private final SecretKey secretKey;
    private final long expiration;

    /* ======================================================
       CONSTRUCTOR
       ====================================================== */

    public TokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expiration
    ) {
        this.secretKey = buildKey(secret);
        this.expiration = expiration;
    }

    private SecretKey buildKey(String secret) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 characters"
            );
        }

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /* ======================================================
       GENERATE TOKEN (MULTI-TENANT SAFE)
       ====================================================== */

    public String generateToken(User user, String schemaName) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Tenant schema cannot be blank");
        }

        if (user.getRole() == null) {
            throw new IllegalStateException("User role cannot be null");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant", schemaName.trim().toLowerCase());

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().getName())
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Overload seguro — usa tenant do usuário
     */
    public String generateToken(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getTenant() == null ||
                user.getTenant().getSchemaName() == null) {

            throw new IllegalStateException("User tenant is not defined");
        }

        return generateToken(user, user.getTenant().getSchemaName());
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    public boolean isValid(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("JWT expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT: {}", e.getMessage());
        }

        return false;
    }

    /* ======================================================
       EXTRACTION
       ====================================================== */

    public String getEmail(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public UUID getUserId(String token) {

        String claim = parseToken(token)
                .getBody()
                .get("userId", String.class);

        if (claim == null) {
            throw new IllegalArgumentException("userId claim missing");
        }

        return UUID.fromString(claim);
    }

    public String getRole(String token) {
        return parseToken(token)
                .getBody()
                .get("role", String.class);
    }

    public String getTenant(String token) {
        return parseToken(token)
                .getBody()
                .get("tenant", String.class);
    }

    public Date getExpiration(String token) {
        return parseToken(token)
                .getBody()
                .getExpiration();
    }

    /* ======================================================
       INTERNAL PARSE
       ====================================================== */

    private Jws<Claims> parseToken(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
    }
}