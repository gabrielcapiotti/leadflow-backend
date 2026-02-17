package com.leadflow.backend.dto.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class UpdateSettingRequest {

    @NotBlank(message = "Vendor name é obrigatório")
    @Size(max = 100, message = "Vendor name deve ter no máximo 100 caracteres")
    private final String vendorName;

    @NotBlank(message = "WhatsApp é obrigatório")
    @Size(max = 15, message = "WhatsApp deve ter no máximo 15 caracteres")
    private final String whatsapp;

    @Size(max = 100, message = "Company name deve ter no máximo 100 caracteres")
    private final String companyName;

    private final String logo;

    @Size(max = 500, message = "Welcome message deve ter no máximo 500 caracteres")
    private final String welcomeMessage;

    public UpdateSettingRequest(
            String vendorName,
            String whatsapp,
            String companyName,
            String logo,
            String welcomeMessage
    ) {
        this.vendorName = vendorName;
        this.whatsapp = whatsapp;
        this.companyName = companyName;
        this.logo = logo;
        this.welcomeMessage = welcomeMessage;
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
