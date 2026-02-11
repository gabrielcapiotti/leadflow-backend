package com.leadflow.backend.controller.role;

import com.leadflow.backend.dto.role.RoleResponse;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /* ==========================
       LIST
       ========================== */

    @GetMapping
    public ResponseEntity<List<RoleResponse>> list() {

        List<RoleResponse> response = roleService.listAll()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /* ==========================
       GET BY ID
       ========================== */

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getById(
            @PathVariable @NonNull Integer id
    ) {
        Role role = roleService.getById(id);
        return ResponseEntity.ok(toResponse(role));
    }

    /* ==========================
       MAPPER
       ========================== */

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName()
        );
    }
}
