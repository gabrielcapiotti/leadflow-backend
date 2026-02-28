package com.leadflow.backend.repository.auth;

import com.leadflow.backend.entities.auth.UserSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository
        extends JpaRepository<UserSession, UUID> {

    /* ======================================================
       FIND ACTIVE SESSION (TENANT SAFE)
       ====================================================== */

    Optional<UserSession> findByTokenIdAndTenantIdAndActiveTrue(
            String tokenId,
            UUID tenantId
    );

    boolean existsByTokenIdAndTenantIdAndActiveTrue(
            String tokenId,
            UUID tenantId
    );

    /* ======================================================
       BULK REVOKE (LOGOUT ALL DEVICES)
       ====================================================== */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE UserSession s
           SET s.active = false,
               s.revokedAt = :revokedAt
         WHERE s.userId = :userId
           AND s.tenantId = :tenantId
           AND s.active = true
    """)
    int revokeAllActiveSessions(UUID userId,
                                UUID tenantId,
                                Instant revokedAt);

    /* ======================================================
       CLEANUP REVOKED SESSIONS
       ====================================================== */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM UserSession s
         WHERE s.active = false
           AND s.revokedAt < :threshold
    """)
    int deleteRevokedBefore(Instant threshold);

    /* ======================================================
       DEVICE LIMIT SUPPORT
       ====================================================== */

    List<UserSession> findByUserIdAndTenantIdAndActiveTrueOrderByCreatedAtAsc(
            UUID userId,
            UUID tenantId
    );

    List<UserSession> findByUserIdAndTenantIdAndActiveTrueOrderByCreatedAtDesc(
            UUID userId,
            UUID tenantId
    );

    Optional<UserSession> findByIdAndUserIdAndTenantIdAndActiveTrue(
            UUID sessionId,
            UUID userId,
            UUID tenantId
    );

    long countByUserIdAndTenantIdAndActiveTrue(
            UUID userId,
            UUID tenantId
    );
}