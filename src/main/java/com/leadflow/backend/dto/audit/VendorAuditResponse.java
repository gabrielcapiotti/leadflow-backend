package com.leadflow.backend.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record VendorAuditResponse(
        UUID id,
        UUID vendorId,
        String userEmail,
        String acao,
        String entityType,
        UUID entidadeId,
        String detalhes,
        Instant createdAt
) {}