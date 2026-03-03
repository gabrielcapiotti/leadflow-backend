package com.leadflow.backend.dto.ai;

import java.util.UUID;

public class ChatRequest {

    private UUID leadId;
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