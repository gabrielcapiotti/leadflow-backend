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
       REQUEST → ENTITY (UPDATE PARCIAL SEGURO)
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

        setting.update(
                resolve(request.getVendorName(), setting.getVendorName()),
                resolve(request.getWhatsapp(), setting.getWhatsapp()),
                resolve(request.getCompanyName(), setting.getCompanyName()),
                resolve(request.getLogo(), setting.getLogo()),
                resolve(request.getWelcomeMessage(), setting.getWelcomeMessage())
        );
    }

    /* ======================================================
       INTERNAL UTIL
       ====================================================== */

    private String resolve(String incoming, String current) {

        if (incoming == null) {
            return current;
        }

        String trimmed = incoming.trim();

        // Evita sobrescrever com string vazia
        if (trimmed.isBlank()) {
            return current;
        }

        return trimmed;
    }
}