package com.leadflow.backend.dto.lead;

import com.leadflow.backend.entities.enums.LeadStatus;

import java.time.LocalDateTime;

public class LeadResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private LeadStatus status;
    private LocalDateTime createdAt;

    public LeadResponse(
            Long id,
            String name,
            String email,
            String phone,
            LeadStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
