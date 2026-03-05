package com.leadflow.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import com.leadflow.backend.service.notification.SendGridEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.domain.auth.PasswordResetToken;
import com.leadflow.domain.auth.repository.PasswordResetTokenRepository;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_EXPIRATION_MINUTES = 15;
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SendGridEmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.frontend.reset-password-path:/reset-password}")
    private String resetPasswordPath;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SendGridEmailService emailService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /* ======================================================
       REQUEST RESET
       ====================================================== */

    /**
     * Gera token seguro de redefinição de senha.
     * Retorna o token RAW apenas para envio por email.
     * Nunca armazena o token em texto plano no banco.
     */
    @Transactional
    public String requestPasswordReset(String email) {

        if (email == null || email.isBlank()) {
            return null; // Anti-enumeração
        }

        String normalizedEmail = email.trim().toLowerCase();

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .map(user -> {

                    // Remove tokens antigos
                    tokenRepository.deleteByUser_Id(user.getId());

                    String rawToken = generateSecureToken();
                    String tokenHash = hash(rawToken);

                    PasswordResetToken entity = new PasswordResetToken(
                            tokenHash,
                            user,
                            LocalDateTime.now()
                                    .plusMinutes(TOKEN_EXPIRATION_MINUTES)
                    );

                    tokenRepository.save(entity);

                        String resetLink = buildResetLink(rawToken);
                        sendPasswordResetEmail(user.getEmail(), resetLink);

                    return rawToken;
                })
                .orElse(null);
    }

    /* ======================================================
       RESET PASSWORD
       ====================================================== */

    /**
     * Realiza redefinição de senha usando token válido.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {

        validatePassword(newPassword);

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String tokenHash = hash(rawToken);

        PasswordResetToken token = tokenRepository
                .findByTokenHashAndUsedFalseAndExpiresAtAfter(
                        tokenHash,
                        LocalDateTime.now()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid or expired token")
                );

        User user = token.getUser();

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.markAsUsed();
        tokenRepository.save(token);

        // Remove quaisquer tokens restantes do usuário
        tokenRepository.deleteByUser_Id(user.getId());
    }

    /* ======================================================
       CLEANUP
       ====================================================== */

    /**
     * Remove tokens expirados.
     * Pode ser executado via @Scheduled.
     */
    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /* ======================================================
       INTERNAL UTILITIES
       ====================================================== */

    /**
     * Gera token criptograficamente seguro (256 bits).
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    /**
     * Gera hash SHA-256 do token.
     */
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

    /**
     * Validação mínima de senha.
     */
    private void validatePassword(String password) {

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters"
            );
        }
    }

    private String buildResetLink(String rawToken) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        String path = resetPasswordPath == null ? "/reset-password" : resetPasswordPath.trim();

        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path + "?token=" + rawToken;
        }

        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path + "?token=" + rawToken;
        }

        return base + path + "?token=" + rawToken;
    }

    private void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "Redefinição de senha";
        String html = """
                <p>Recebemos uma solicitação para redefinir sua senha.</p>
                <p>Clique no link abaixo para continuar (válido por 15 minutos):</p>
                <p><a href=\"%s\">Redefinir senha</a></p>
                <p>Se você não solicitou esta alteração, ignore este e-mail.</p>
                """.formatted(resetLink);

        try {
            emailService.sendEmail(to, subject, html);
        } catch (RuntimeException ex) {
            log.warn("event=password_reset_email_failed to={}", to, ex);
        }
    }
}