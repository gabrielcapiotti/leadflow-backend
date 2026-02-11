package com.leadflow.backend.service.settings;

import com.leadflow.backend.entities.Setting;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.settings.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SettingService {

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /* ==========================
       CREATE / UPDATE
       ========================== */

    /**
     * Cria ou atualiza as configurações do usuário autenticado.
     */
    @Transactional
    public Setting saveOrUpdate(
            User user,
            String vendorName,
            String whatsapp,
            String companyName,
            String logo,
            String welcomeMessage
    ) {

        return settingRepository.findByUser(user)
                .map(existing -> {
                    existing.setVendorName(vendorName);
                    existing.setWhatsapp(whatsapp);
                    existing.setCompanyName(companyName);
                    existing.setLogo(logo);
                    existing.setWelcomeMessage(welcomeMessage);
                    existing.setDeletedAt(null); // reativa se estava soft-deleted
                    return settingRepository.save(existing);
                })
                .orElseGet(() -> settingRepository.save(
                        new Setting(
                                user,
                                vendorName,
                                whatsapp,
                                companyName,
                                logo,
                                welcomeMessage
                        )
                ));
    }

    /* ==========================
       READ
       ========================== */

    /**
     * Retorna as configurações do usuário autenticado.
     */
    @Transactional(readOnly = true)
    public Setting getByUser(User user) {
        return settingRepository.findByUser(user)
                .orElseThrow(() ->
                        new IllegalStateException("Settings not found for user id=" + user.getId())
                );
    }

    /* ==========================
       DELETE (SOFT DELETE)
       ========================== */

    /**
     * Soft delete das configurações do usuário.
     */
    @Transactional
    public void softDelete(User user) {
        Setting setting = getByUser(user);
        setting.setDeletedAt(LocalDateTime.now());
        settingRepository.save(setting);
    }
}