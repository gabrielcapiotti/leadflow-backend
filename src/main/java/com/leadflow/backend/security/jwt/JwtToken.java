package com.leadflow.backend.security.jwt;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable representation of a generated JWT and its metadata.
 *
 * This object contains only transport-safe information and does not expose
 * JWT claims directly.
 *
 * Used by:
 * - JwtService
 * - Authentication responses
 * - Security filters
 */
public final class JwtToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String token;
    private final String tokenId;
    private final Instant expiresAt;

    public JwtToken(String token,
                    String tokenId,
                    Instant expiresAt) {

        this.token = validateToken(token);
        this.tokenId = validateTokenId(tokenId);
        this.expiresAt = Objects.requireNonNull(
                expiresAt,
                "expiresAt cannot be null"
        );
    }

    private static String validateToken(String token) {

        Objects.requireNonNull(token, "token cannot be null");

        if (token.isBlank()) {
            throw new IllegalArgumentException("token cannot be blank");
        }

        return token;
    }

    private static String validateTokenId(String tokenId) {

        Objects.requireNonNull(tokenId, "tokenId cannot be null");

        if (tokenId.isBlank()) {
            throw new IllegalArgumentException("tokenId cannot be blank");
        }

        return tokenId;
    }

    public String getToken() {
        return token;
    }

    public String getTokenId() {
        return tokenId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Returns true if the token is already expired.
     */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /**
     * Returns true if the token is still valid.
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Remaining lifetime of the token.
     */
    public Duration getRemainingLifetime() {

        if (isExpired()) {
            return Duration.ZERO;
        }

        return Duration.between(Instant.now(), expiresAt);
    }

    @Override
    public String toString() {

        return "JwtToken{" +
                "tokenId='" + tokenId + '\'' +
                ", expiresAt=" + expiresAt +
                '}';
        // Token intentionally omitted for security
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (!(o instanceof JwtToken that)) return false;

        return tokenId.equals(that.tokenId);
    }

    @Override
    public int hashCode() {
        return tokenId.hashCode();
    }
}