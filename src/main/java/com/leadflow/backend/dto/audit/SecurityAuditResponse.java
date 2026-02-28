package com.leadflow.backend.dto.audit;

import com.leadflow.backend.entities.audit.SecurityAction;

import java.time.LocalDateTime;
import java.util.UUID;

public record SecurityAuditResponse(
        UUID id,
        SecurityAction action,
        String email,
        String tenant,
        boolean success,
        String ipAddress,
        String userAgent,
        String correlationId,
        LocalDateTime createdAt
) {}