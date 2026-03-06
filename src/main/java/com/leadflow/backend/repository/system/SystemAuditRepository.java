package com.leadflow.backend.repository.system;

import com.leadflow.backend.entities.system.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SystemAuditRepository
        extends JpaRepository<SystemAuditLog, UUID> {
}