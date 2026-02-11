package com.leadflow.backend.controller.settings;

import com.leadflow.backend.dto.settings.SettingResponse;
import com.leadflow.backend.dto.settings.UpdateSettingRequest;
import com.leadflow.backend.entities.Setting;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.settings.SettingService;
import com.leadflow.backend.service.settings.SettingMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;
    private final SettingMapper settingMapper;

    public SettingController(
            SettingService settingService,
            SettingMapper settingMapper
    ) {
        this.settingService = settingService;
        this.settingMapper = settingMapper;
    }

    /* ==========================
       READ (CURRENT USER)
       ========================== */

    @GetMapping
    public ResponseEntity<SettingResponse> get(
            @AuthenticationPrincipal User currentUser
    ) {
        Setting setting = settingService.getByUser(currentUser);
        return ResponseEntity.ok(settingMapper.toResponse(setting));
    }

    /* ==========================
       CREATE / UPDATE
       ========================== */

    @PutMapping
    public ResponseEntity<SettingResponse> saveOrUpdate(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateSettingRequest request
    ) {
        Setting setting = settingService.saveOrUpdate(
                currentUser,
                request.getVendorName(),
                request.getWhatsapp(),
                request.getCompanyName(),
                request.getLogo(),
                request.getWelcomeMessage()
        );

        return ResponseEntity.ok(settingMapper.toResponse(setting));
    }

    /* ==========================
       DELETE (SOFT)
       ========================== */

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser
    ) {
        settingService.softDelete(currentUser);
        return ResponseEntity.noContent().build();
    }
}
