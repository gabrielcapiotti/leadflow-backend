package com.leadflow.backend.repository.auth;

import com.leadflow.backend.entities.auth.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    /* ======================================================
       READ
       ====================================================== */

    /**
     * Busca token válido (não revogado).
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * Busca token mesmo que revogado (para reuse detection).
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /* ======================================================
       DELETE / CLEANUP
       ====================================================== */

    /**
     * Remove todos os tokens de um usuário.
     * Utilizado em logout global ou rotação.
     */
    @Modifying
    void deleteByUser_Id(UUID userId);

    /**
     * Remove tokens expirados.
     * Ideal para execução via @Scheduled.
     */
    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime now);

    /**
     * Remove todos tokens revogados (limpeza preventiva).
     */
    @Modifying
    void deleteByRevokedTrue();
}