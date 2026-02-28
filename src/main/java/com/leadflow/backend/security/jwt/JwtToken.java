package com.leadflow.backend.security.jwt;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable representation of a generated JWT and its metadata.
 *
 * This object contains only transport-safe information.
 * It does NOT expose claims directly.
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

        this.token = Objects.requireNonNull(token, "token cannot be null");
        this.tokenId = Objects.requireNonNull(tokenId, "tokenId cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");

        if (token.isBlank()) {
            throw new IllegalArgumentException("token cannot be blank");
        }

        if (tokenId.isBlank()) {
            throw new IllegalArgumentException("tokenId cannot be blank");
        }
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
     * Indicates whether the token is already expired
     * based on the provided reference time.
     */
    public boolean isExpired(Instant referenceTime) {
        return expiresAt.isBefore(referenceTime);
    }

    @Override
    public String toString() {
        return "JwtToken{" +
                "tokenId='" + tokenId + '\'' +
                ", expiresAt=" + expiresAt +
                '}';
        // Intentionally NOT printing the token itself
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