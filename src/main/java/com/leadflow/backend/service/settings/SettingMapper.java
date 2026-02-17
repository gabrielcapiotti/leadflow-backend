package com.leadflow.backend.service.settings;

import com.leadflow.backend.dto.settings.SettingResponse;
import com.leadflow.backend.dto.settings.UpdateSettingRequest;
import com.leadflow.backend.entities.Setting;
import org.springframework.stereotype.Component;

@Component
public class SettingMapper {

    /* ======================================================
       ENTITY → RESPONSE
       ====================================================== */

    public SettingResponse toResponse(Setting setting) {

        if (setting == null) {
            throw new IllegalArgumentException("Setting cannot be null");
        }

        return new SettingResponse(
                setting.getId(),
                setting.getVendorName(),
                setting.getWhatsapp(),
                setting.getCompanyName(),
                setting.getLogo(),
                setting.getWelcomeMessage()
        );
    }

    /* ======================================================
       REQUEST → ENTITY (UPDATE SEGURO)
       ====================================================== */

    public void updateEntity(
            Setting setting,
            UpdateSettingRequest request
    ) {

        if (setting == null) {
            throw new IllegalArgumentException("Setting cannot be null");
        }

        if (request == null) {
            throw new IllegalArgumentException("Update request cannot be null");
        }

        // Usa valores atuais se campo vier null (update parcial)
        String vendorName = request.getVendorName() != null
                ? request.getVendorName()
                : setting.getVendorName();

        String whatsapp = request.getWhatsapp() != null
                ? request.getWhatsapp()
                : setting.getWhatsapp();

        String companyName = request.getCompanyName() != null
                ? request.getCompanyName()
                : setting.getCompanyName();

        String logo = request.getLogo() != null
                ? request.getLogo()
                : setting.getLogo();

        String welcomeMessage = request.getWelcomeMessage() != null
                ? request.getWelcomeMessage()
                : setting.getWelcomeMessage();

        // Delegação correta para regra de domínio
        setting.update(
                vendorName,
                whatsapp,
                companyName,
                logo,
                welcomeMessage
        );
    }
}
