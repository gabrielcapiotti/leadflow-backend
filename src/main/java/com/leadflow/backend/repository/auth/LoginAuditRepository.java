package com.leadflow.backend.repository.auth;

import com.leadflow.backend.entities.auth.LoginAudit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LoginAuditRepository
        extends JpaRepository<LoginAudit, UUID> {

    /* ======================================================
       USER HISTORY
       ====================================================== */

    List<LoginAudit> findByUserIdAndTenantIdOrderByCreatedAtDesc(
            UUID userId,
            UUID tenantId
    );

    /* ======================================================
       EMAIL HISTORY (FAILED ATTEMPTS)
       ====================================================== */

    List<LoginAudit> findByEmailAndTenantIdAndSuccessFalseOrderByCreatedAtDesc(
            String email,
            UUID tenantId
    );

    /* ======================================================
       FAILED ATTEMPTS WINDOW (BRUTE FORCE DETECTION)
       ====================================================== */

    long countByEmailAndTenantIdAndSuccessFalseAndCreatedAtAfter(
            String email,
            UUID tenantId,
            Instant threshold
    );

    /* ======================================================
       CLEANUP OLD RECORDS (OPTIONAL)
       ====================================================== */

    void deleteByCreatedAtBefore(Instant threshold);
}