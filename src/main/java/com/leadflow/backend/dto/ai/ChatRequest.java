package com.leadflow.backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class ChatRequest {

    @NotNull(message = "leadId é obrigatório")
    private UUID leadId;

    @NotBlank(message = "message é obrigatória")
    @Size(max = 2000, message = "message deve ter no máximo 2000 caracteres")
    private String message;

    public UUID getLeadId() {
        return leadId;
    }

    public void setLeadId(UUID leadId) {
        this.leadId = leadId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}