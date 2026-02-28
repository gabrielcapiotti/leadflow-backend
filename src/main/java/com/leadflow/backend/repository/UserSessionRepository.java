package com.leadflow.backend.repository;

import com.leadflow.backend.entities.auth.UserSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UserSessionRepository
        extends JpaRepository<UserSession, UUID> {

    /* ======================================================
       CLEANUP REVOKED SESSIONS
       ====================================================== */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        DELETE FROM UserSession s
         WHERE s.active = false
           AND s.revokedAt < :threshold
    """)
    int deleteRevokedBefore(Instant threshold);
}