package com.leadflow.backend.dto.settings;

public class SettingResponse {

    private Long id;
    private String vendorName;
    private String whatsapp;
    private String companyName;
    private String logo;
    private String welcomeMessage;

    public SettingResponse(
            Long id,
            String vendorName,
            String whatsapp,
            String companyName,
            String logo,
            String welcomeMessage
    ) {
        this.id = id;
        this.vendorName = vendorName;
        this.whatsapp = whatsapp;
        this.companyName = companyName;
        this.logo = logo;
        this.welcomeMessage = welcomeMessage;
    }

    public Long getId() {
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
