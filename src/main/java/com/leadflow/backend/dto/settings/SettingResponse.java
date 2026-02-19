package com.leadflow.backend.dto.settings;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SettingResponse {

    private final UUID id;
    private final String vendorName;
    private final String whatsapp;
    private final String companyName;
    private final String logo;
    private final String welcomeMessage;

    public SettingResponse(
            UUID id,
            String vendorName,
            String whatsapp,
            String companyName,
            String logo,
            String welcomeMessage
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.vendorName = Objects.requireNonNull(vendorName, "vendorName cannot be null");
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.companyName = companyName;
        this.logo = logo;
        this.welcomeMessage = welcomeMessage;
    }

    public UUID getId() {
        return id;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getLogo() {
        return logo;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }
}
