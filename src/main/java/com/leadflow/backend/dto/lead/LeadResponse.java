package com.leadflow.backend.dto.lead;

import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;

import java.time.LocalDateTime;
import java.util.UUID;

public class LeadResponse {

    private final UUID id;
    private final String name;
    private final String email;
    private final String phone;
    private final LeadStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /* ======================================================
       CONSTRUCTOR FROM ENTITY
       ====================================================== */

    public LeadResponse(Lead lead) {

        if (lead == null) {
            throw new IllegalArgumentException("Lead cannot be null");
        }

        this.id = lead.getId();
        this.name = lead.getName();
        this.email = lead.getEmail();
        this.phone = lead.getPhone();
        this.status = lead.getStatus();
        this.createdAt = lead.getCreatedAt();
        this.updatedAt = lead.getUpdatedAt();
    }

    /* ======================================================
       OPTIONAL DIRECT CONSTRUCTOR (useful for tests/mappers)
       ====================================================== */

    public LeadResponse(
            UUID id,
            String name,
            String email,
            String phone,
            LeadStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /* ======================================================
       GETTERS
       ====================================================== */

    public UUID getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getPhone() { return phone; }

    public LeadStatus getStatus() { return status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
