package com.leadflow.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.security.jwt.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    private static final Logger logger =
            LoggerFactory.getLogger(TokenService.class);

    private static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.HS256;
    private static final int MIN_SECRET_LENGTH = 32; // 256 bits

    private final SecretKey secretKey;
    private final long expiration;

    public TokenService() {
        this.secretKey = Keys.hmacShaKeyFor("default-secret-key-32-characters".getBytes(StandardCharsets.UTF_8));
        this.expiration = 3600000; // Default to 1 hour
    }

    public TokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expiration
    ) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    "JWT secret must have at least 32 characters (256 bits)"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expiration = expiration;
    }

    public TokenService(JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.secretKey = Keys.hmacShaKeyFor("default-secret-key-32-characters".getBytes(StandardCharsets.UTF_8));
        this.expiration = 3600000; // Default to 1 hour
    }

    /* ==========================
       GENERATE TOKEN
       ========================== */

    public String generateToken(User user, String tenant) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().getName())
                .claim("tenant", tenant) // Adiciona o tenant ao token
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(secretKey, ALGORITHM)
                .compact();
    }

    /* ==========================
       VALIDATION
       ========================== */

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /* ==========================
       EXTRACTION
       ========================== */

    public String getEmail(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public Long getUserId(String token) {
        return parseToken(token).getBody().get("userId", Long.class);
    }

    public String getRole(String token) {
        return parseToken(token).getBody().get("role", String.class);
    }

    /* ==========================
       INTERNAL
       ========================== */

    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
    }
}
