package com.leadflow.backend.repository.audit;

import com.leadflow.backend.entities.audit.SecurityAuditLog;
import com.leadflow.backend.entities.audit.SecurityAction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SecurityAuditLogRepository
        extends JpaRepository<SecurityAuditLog, UUID>,
                JpaSpecificationExecutor<SecurityAuditLog> {

    Page<SecurityAuditLog> findByEmailContainingIgnoreCase(
            String email,
            Pageable pageable
    );

    Page<SecurityAuditLog> findByAction(
            SecurityAction action,
            Pageable pageable
    );

    Page<SecurityAuditLog> findByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}