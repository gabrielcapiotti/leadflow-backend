package com.leadflow.backend.dto.settings;

import jakarta.validation.constraints.NotBlank;

public class UpdateSettingRequest {

    @NotBlank
    private String vendorName;

    @NotBlank
    private String whatsapp;

    private String companyName;
    private String logo;
    private String welcomeMessage;

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
