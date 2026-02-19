package com.leadflow.backend.controller.settings;

import com.leadflow.backend.dto.settings.SettingResponse;
import com.leadflow.backend.dto.settings.UpdateSettingRequest;
import com.leadflow.backend.entities.Setting;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.settings.SettingMapper;
import com.leadflow.backend.service.settings.SettingService;
import com.leadflow.backend.service.user.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;
    private final SettingMapper settingMapper;
    private final UserService userService;

    public SettingController(
            SettingService settingService,
            SettingMapper settingMapper,
            UserService userService
    ) {
        this.settingService = settingService;
        this.settingMapper = settingMapper;
        this.userService = userService;
    }

    /* ======================================================
       GET (isolado por usuário)
       ====================================================== */

    @GetMapping
    public ResponseEntity<SettingResponse> get(
            @AuthenticationPrincipal UserDetails principal
    ) {

        User user = resolveUser(principal);

        Setting setting = settingService.getByUser(user);

        return ResponseEntity.ok(settingMapper.toResponse(setting));
    }

    /* ======================================================
       GET BY ID (ADMIN / uso interno)
       ====================================================== */

    @GetMapping("/{id}")
    public ResponseEntity<SettingResponse> getById(
            @PathVariable UUID id
    ) {

        Setting setting = settingService.getById(id);

        return ResponseEntity.ok(settingMapper.toResponse(setting));
    }

    /* ======================================================
       SAVE OR UPDATE
       ====================================================== */

    @PutMapping
    public ResponseEntity<SettingResponse> saveOrUpdate(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateSettingRequest request
    ) {

        User user = resolveUser(principal);

        Setting setting = settingService.saveOrUpdate(
                user,
                request.getVendorName(),
                request.getWhatsapp(),
                request.getCompanyName(),
                request.getLogo(),
                request.getWelcomeMessage()
        );

        return ResponseEntity.ok(settingMapper.toResponse(setting));
    }

    /* ======================================================
       DELETE (soft delete por usuário)
       ====================================================== */

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal
    ) {

        User user = resolveUser(principal);

        settingService.softDelete(user);

        return ResponseEntity.noContent().build();
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private User resolveUser(UserDetails principal) {

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "User not authenticated"
            );
        }

        return userService.getActiveByEmail(principal.getUsername());
    }
}
