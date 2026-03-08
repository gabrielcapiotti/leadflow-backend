package com.leadflow.backend.controller.role;

import com.leadflow.backend.dto.role.RoleResponse;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.service.RoleService;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')") // 🔒 Protege todos os endpoints da classe
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService =
                Objects.requireNonNull(roleService, "RoleService must not be null");
    }

    /* ======================================================
       LIST ALL ROLES
       ====================================================== */

    @GetMapping
    public ResponseEntity<List<RoleResponse>> list() {

        List<RoleResponse> response = roleService.listAll()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       GET ROLE BY UUID
       ====================================================== */

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getById(
            @PathVariable @NonNull UUID id
    ) {

        UUID safeId =
                Objects.requireNonNull(id, "Role id must not be null");

        Role role = roleService.getById(safeId);

        if (role == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toResponse(role));
    }

    /* ======================================================
       MAPPER
       ====================================================== */

    private RoleResponse toResponse(Role role) {

        Role safeRole =
                Objects.requireNonNull(role, "Role must not be null");

        return new RoleResponse(
                safeRole.getId(),
                safeRole.getName()
        );
    }
}