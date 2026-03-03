package com.leadflow.backend.dto.vendor;

public record AlertDTO(
        String tipo,
        String mensagem,
        String leadId
) {
}
