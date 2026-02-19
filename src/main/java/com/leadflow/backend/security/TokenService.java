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
import java.util.UUID;

@Service
public class TokenService {

    private static final Logger logger =
            LoggerFactory.getLogger(TokenService.class);

    private final SecretKey secretKey;
    private final long expiration;

    /* ======================================================
       CONSTRUTOR SPRING (produção)
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
       GENERATE TOKEN (com tenant)
       ====================================================== */

    public String generateToken(User user, String tenant) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().getName())
                .claim("tenant", tenant)
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ======================================================
       OVERLOAD (caso produção use user.getTenantId())
       ====================================================== */

    public String generateToken(User user) {
        return generateToken(user, "public");
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /* ======================================================
       EXTRACTION
       ====================================================== */

    public String getEmail(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public UUID getUserId(String token) {
        Object claim = parseToken(token).getBody().get("userId");

        if (claim == null) {
            throw new IllegalArgumentException("userId claim not found in token");
        }

        return UUID.fromString(claim.toString());
    }


    public String getRole(String token) {
        return parseToken(token).getBody().get("role", String.class);
    }

    public String getTenant(String token) {
        return parseToken(token).getBody().get("tenant", String.class);
    }

    /* ======================================================
       INTERNAL PARSE
       ====================================================== */

    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
    }
}
