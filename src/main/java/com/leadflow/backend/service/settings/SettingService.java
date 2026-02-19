package com.leadflow.backend.service.settings;

import com.leadflow.backend.entities.Setting;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.settings.SettingRepository;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingService {

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /* ======================================================
       CREATE OR UPDATE
       ====================================================== */

    @Transactional
    public Setting saveOrUpdate(
            User user,
            String vendorName,
            String whatsapp,
            String companyName,
            String logo,
            String welcomeMessage
    ) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        Setting setting = settingRepository
                .findByUser(user)
                .orElse(null);

        if (setting == null) {
            // Criação correta usando construtor completo
            setting = new Setting(
                    user,
                    vendorName,
                    whatsapp,
                    companyName,
                    logo,
                    welcomeMessage
            );
        } else {

            // Se estava soft deleted, reativa implicitamente
            if (setting.isDeleted()) {
                setting = new Setting(
                        user,
                        vendorName != null ? vendorName : setting.getVendorName(),
                        whatsapp != null ? whatsapp : setting.getWhatsapp(),
                        companyName != null ? companyName : setting.getCompanyName(),
                        logo != null ? logo : setting.getLogo(),
                        welcomeMessage != null ? welcomeMessage : setting.getWelcomeMessage()
                );
            } else {

                // Update seguro usando método de domínio
                setting.update(
                        vendorName != null ? vendorName : setting.getVendorName(),
                        whatsapp != null ? whatsapp : setting.getWhatsapp(),
                        companyName != null ? companyName : setting.getCompanyName(),
                        logo != null ? logo : setting.getLogo(),
                        welcomeMessage != null ? welcomeMessage : setting.getWelcomeMessage()
                );
            }
        }

        return settingRepository.save(setting);
    }

    /* ======================================================
       READ
       ====================================================== */

    @Transactional(readOnly = true)
    public Setting getByUser(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return settingRepository.findByUser(user)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Settings not found for user id=" + user.getId()
                        )
                );
    }

    /* ======================================================
       SOFT DELETE
       ====================================================== */

    @Transactional
    public void softDelete(User user) {

        Setting setting = getByUser(user);

        setting.softDelete();
    }

    public Setting getById(UUID id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }
}
