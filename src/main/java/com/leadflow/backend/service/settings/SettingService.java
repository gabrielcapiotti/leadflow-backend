package com.leadflow.backend.service.settings;

import com.leadflow.backend.entities.Setting;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.settings.SettingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

            // 🆕 Criação
            setting = new Setting(
                    user,
                    vendorName,
                    whatsapp,
                    companyName,
                    logo,
                    welcomeMessage
            );

        } else {

            // 🔄 Reativação segura (sem recriar entidade)
            if (setting.isDeleted()) {
                setting.restore();
            }

            // 🔄 Atualização parcial segura
            setting.update(
                    vendorName != null ? vendorName : setting.getVendorName(),
                    whatsapp != null ? whatsapp : setting.getWhatsapp(),
                    companyName != null ? companyName : setting.getCompanyName(),
                    logo != null ? logo : setting.getLogo(),
                    welcomeMessage != null ? welcomeMessage : setting.getWelcomeMessage()
            );
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
                .filter(setting -> !setting.isDeleted())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Settings not found for user id=" + user.getId()
                        )
                );
    }

    @Transactional(readOnly = true)
    public Setting getById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException("Setting id cannot be null");
        }

        return settingRepository.findById(id)
                .filter(setting -> !setting.isDeleted())
                .orElseThrow(() ->
                        new IllegalArgumentException("Setting not found"));
    }

    /* ======================================================
       SOFT DELETE
       ====================================================== */

    @Transactional
    public void softDelete(User user) {

        Setting setting = getByUser(user);

        if (!setting.isDeleted()) {
            setting.softDelete();
            settingRepository.save(setting);
        }
    }
}