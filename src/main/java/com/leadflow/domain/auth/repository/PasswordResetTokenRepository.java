package com.leadflow.domain.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import com.leadflow.domain.auth.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Busca token válido:
     * - Hash correspondente
     * - Não utilizado
     * - Não expirado
     */
    Optional<PasswordResetToken>
    findByTokenHashAndUsedFalseAndExpiresAtAfter(
            String tokenHash,
            LocalDateTime now
    );

    /**
     * Remove tokens expirados.
     */
    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Remove todos os tokens de um usuário.
     */
    @Modifying
    void deleteByUser_Id(UUID userId);
}