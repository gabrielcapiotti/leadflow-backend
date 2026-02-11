package com.leadflow.backend.service.settings;

import com.leadflow.backend.dto.settings.SettingResponse;
import com.leadflow.backend.dto.settings.UpdateSettingRequest;
import com.leadflow.backend.entities.Setting;
import org.springframework.stereotype.Component;

@Component
public class SettingMapper {

    /* ==========================
       ENTITY → RESPONSE
       ========================== */

    public SettingResponse toResponse(Setting setting) {
        if (setting == null) {
            return null;
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

    /* ==========================
       REQUEST → ENTITY
       ========================== */

    public void updateEntity(
            Setting setting,
            UpdateSettingRequest request
    ) {
        if (setting == null || request == null) {
            return;
        }

        setting.setVendorName(request.getVendorName());
        setting.setWhatsapp(request.getWhatsapp());
        setting.setCompanyName(request.getCompanyName());
        setting.setLogo(request.getLogo());
        setting.setWelcomeMessage(request.getWelcomeMessage());
    }
}
