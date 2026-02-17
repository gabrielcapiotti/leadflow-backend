package com.leadflow.backend.dto.lead;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.leadflow.backend.entities.enums.LeadStatus;

import java.time.LocalDateTime;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LeadStatusHistoryResponse {

    private final Long id;
    private final String status;
    private final String updatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime changedAt;

    public LeadStatusHistoryResponse(
            Long id,
            LeadStatus status,
            LocalDateTime changedAt,
            String updatedBy
    ) {

        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null").name();
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt cannot be null");
        this.updatedBy = updatedBy; // pode ser null ou "SYSTEM"
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
