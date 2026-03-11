package com.leadflow.backend.security.jwt;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.util.LogSanitizer;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Service
@ConditionalOnProperty(
        name = "security.jwt.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class JwtService implements InitializingBean {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtService.class);

    private final String secret;
    private final long expirationMillis;
    private final String issuer;
    private final Clock clock;

    private Key signingKey;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expirationMillis,
            @Value("${security.jwt.issuer:leadflow}") String issuer,
            Clock clock
    ) {
        this.secret = secret;
        this.expirationMillis = expirationMillis;
        this.issuer = issuer;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public void afterPropertiesSet() {
        if (secret == null || secret.isBlank()) {
            logger.error("JWT secret is null or blank");
            throw new IllegalStateException("JWT secret must not be null or blank");
        }

        if (secret.length() < 32) {
            logger.error("JWT secret is too short: {} characters", secret.length());
            throw new IllegalStateException(
                    "JWT secret must be at least 256 bits (32 characters)"
            );
        }

        if (expirationMillis <= 0) {
            logger.error("JWT expiration time is invalid: {}", expirationMillis);
            throw new IllegalStateException(
                    "JWT expiration must be a positive value"
            );
        }

        if (issuer == null || issuer.isBlank()) {
            logger.error("JWT issuer is null or blank");
            throw new IllegalStateException("JWT issuer must not be blank");
        }

        this.signingKey =
                Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        logger.debug("JWT secret successfully loaded");
        logger.debug("JWT Expiration time: {} ms", expirationMillis);
        logger.debug("JWT Issuer: {}", issuer);
    }

    /* ====================================================== */

    public JwtToken generateToken(User user, String tenantSchema) {

        validateUser(user);
        validateTenant(tenantSchema);

        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusMillis(expirationMillis);
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .setId(tokenId)
                .setSubject(user.getEmail())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().getName())
                .claim("tenant", tenantSchema.trim().toLowerCase())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return new JwtToken(token, tokenId, expiresAt);
    }

    /* ====================================================== */

    public boolean isValid(String token) {

        if (token == null || token.isBlank()) {
            logger.warn("Token is null or blank");
            return false;
        }

        try {
            extractAllClaims(token);
            logger.debug("Token is valid: {}", LogSanitizer.sanitize(token));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT detected: {}", LogSanitizer.sanitize(e.getMessage()));
            return false;
        }
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails,
            UUID expectedUserId,
            String expectedTenant
    ) {

        if (!isValid(token)) {
            logger.debug("Token is invalid (failed signature/expiration check)");
            return false;
        }

        try {

            String email = extractEmail(token);
            UUID tokenUserId = extractUserId(token);
            String tokenTenant = extractTenant(token);

            boolean emailMatches = Objects.equals(email, userDetails.getUsername());
            boolean userIdMatches = Objects.equals(tokenUserId, expectedUserId);
            boolean tenantMatches = tokenTenant.equalsIgnoreCase(expectedTenant);
            
            if (!emailMatches) {
                logger.debug("Email mismatch: token={}, expected={}", 
                    LogSanitizer.sanitize(email),
                    LogSanitizer.sanitize(userDetails.getUsername()));
            }
            if (!userIdMatches) {
                logger.debug("UserId mismatch: token={}, expected={}", 
                    LogSanitizer.sanitize(tokenUserId.toString()),
                   LogSanitizer.sanitize(expectedUserId.toString()));
            }
            if (!tenantMatches) {
                logger.debug("Tenant mismatch: token={}, expected={}", 
                    LogSanitizer.sanitize(tokenTenant),
                    LogSanitizer.sanitize(expectedTenant));
            }

            return emailMatches && userIdMatches && tenantMatches;

        } catch (Exception e) {
            logger.warn("Token context validation failed: {}", LogSanitizer.sanitize(e.getMessage()));
            return false;
        }
    }

    /* ====================================================== */

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
        String value = extractClaim(token,
                claims -> claims.get("userId", String.class));

        if (value == null) {
            throw new IllegalStateException("userId claim missing");
        }

        return UUID.fromString(value);
    }

    public String extractRole(String token) {
        return extractClaim(token,
                claims -> claims.get("role", String.class));
    }

    public String extractTenant(String token) {
        return extractClaim(token,
                claims -> claims.get("tenant", String.class));
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /* ====================================================== */

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

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getId() == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalStateException("User email cannot be blank");
        }
        if (user.getRole() == null) {
            throw new IllegalStateException("User role cannot be null");
        }
    }

    private void validateTenant(String tenantSchema) {
        if (tenantSchema == null || tenantSchema.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant schema cannot be blank"
            );
        }
    }
}