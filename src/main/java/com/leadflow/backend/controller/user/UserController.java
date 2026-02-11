package com.leadflow.backend.controller.user;

import com.leadflow.backend.dto.user.UpdateUserRequest;
import com.leadflow.backend.dto.user.UserResponse;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /* ==========================
       LIST
       ========================== */

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {

        List<UserResponse> response = userService.listActiveUsers()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /* ==========================
       UPDATE
       ========================== */

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        Integer roleId = Objects.requireNonNull(request.getRoleId(), "roleId");
        User user = userService.updateUser(
                id,
                request.getName(),
                request.getEmail(),
            roleId
        );

        return ResponseEntity.ok(toResponse(user));
    }

    /* ==========================
       DELETE (SOFT)
       ========================== */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NonNull Long id) {
        userService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /* ==========================
       MAPPER
       ========================== */

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName()
        );
    }
}
