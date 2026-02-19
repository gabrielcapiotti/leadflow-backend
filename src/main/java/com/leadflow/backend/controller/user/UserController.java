package com.leadflow.backend.controller.user;

import com.leadflow.backend.dto.user.UpdateUserRequest;
import com.leadflow.backend.dto.user.UserResponse;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.user.UserService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /* ======================================================
       LIST (PAGINADO)
       ====================================================== */

    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(Pageable pageable) {

        Page<UserResponse> response = userService
                .listActiveUsers(pageable)
                .map(this::toResponse);

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       GET BY ID
       ====================================================== */

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(
            @PathVariable @NonNull UUID id
    ) {

        User user = userService.getById(id);
        return ResponseEntity.ok(toResponse(user));
    }

    /* ======================================================
       UPDATE
       ====================================================== */

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        User user = userService.updateUser(
                id,
                request.getName(),
                request.getEmail(),
                request.getRoleId()
        );

        return ResponseEntity.ok(toResponse(user));
    }

    /* ======================================================
       DELETE (SOFT)
       ====================================================== */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @NonNull UUID id
    ) {

        userService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /* ======================================================
       MAPPER
       ====================================================== */

    private UserResponse toResponse(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName()
        );
    }
}
