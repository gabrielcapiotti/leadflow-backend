package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.auth.RefreshToken;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.auth.RefreshTokenRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int REFRESH_DAYS = 7;

    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    /* ======================================================
       GENERATE NEW REFRESH TOKEN
       ====================================================== */

    @Transactional
    public String generate(User user) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Invalid user");
        }

        String rawToken = generateSecureToken();
        String tokenHash = hash(rawToken);

        RefreshToken entity = new RefreshToken(
                tokenHash,
                user,
                LocalDateTime.now().plusDays(REFRESH_DAYS)
        );

        repository.save(entity);

        return rawToken;
    }

    /* ======================================================
       VALIDATE + ROTATE (SECURE ROTATION)
       ====================================================== */

    @Transactional
    public RotationResult validateAndRotate(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String tokenHash = hash(rawToken);

        RefreshToken token = repository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        // 🔒 REUSE DETECTION
        if (token.isRevoked()) {
            // Se refresh já foi usado antes → ataque
            repository.deleteByUser_Id(token.getUser().getId());
            throw new IllegalStateException("Refresh token reuse detected");
        }

        if (token.isExpired()) {
            token.revoke();
            repository.save(token);
            throw new IllegalArgumentException("Refresh token expired");
        }

        // 🔁 ROTATION
        token.revoke();
        repository.save(token);

        User user = token.getUser();

        String newRefresh = generate(user);

        return new RotationResult(user, newRefresh);
    }

    /* ======================================================
       CLEANUP
       ====================================================== */

    @Transactional
    public void cleanupExpiredTokens() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
        repository.deleteByRevokedTrue();
    }

    /* ======================================================
       INTERNAL UTILITIES
       ====================================================== */

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Token hashing failed", e);
        }
    }

    /* ======================================================
       RESULT WRAPPER
       ====================================================== */

    public record RotationResult(User user, String newRefreshToken) {}
}