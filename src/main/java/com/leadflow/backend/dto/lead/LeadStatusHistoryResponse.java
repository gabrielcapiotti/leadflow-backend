package com.leadflow.backend.dto.lead;

import java.time.LocalDateTime;

public class LeadStatusHistoryResponse {

    private Long id;
    private String status;
    private String updatedBy;
    private LocalDateTime changedAt;

    public LeadStatusHistoryResponse(
            Long id,
            String status,
            String updatedBy,
            LocalDateTime changedAt
    ) {
        this.id = id;
        this.status = status;
        this.updatedBy = updatedBy;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
