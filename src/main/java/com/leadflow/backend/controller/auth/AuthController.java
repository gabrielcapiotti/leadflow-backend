package com.leadflow.backend.controller.auth;

import com.leadflow.backend.dto.auth.AuthResponse;
import com.leadflow.backend.dto.auth.LoginRequest;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.dto.user.UserResponse;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.auth.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /* ======================================================
       REGISTER
       ====================================================== */

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        String schemaName = requireTenant();

        User user = authService.registerUser(
                request.name(),
                request.email(),
                request.password()
        );

        String token = jwtService.generateToken(user, schemaName);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse(token));
    }

    /* ======================================================
       LOGIN
       ====================================================== */

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        String schemaName = requireTenant();

        User user = authService.authenticateUser(
                request.email(),
                request.password()
        );

        String token = jwtService.generateToken(user, schemaName);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    /* ======================================================
       ME
       ====================================================== */

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                    "User not authenticated"
            );
        }

        if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Invalid authentication principal"
            );
        }

        User user = authService.findByEmail(userDetails.getUsername());

        UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName()
        );

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private String requireTenant() {

        String schema = TenantContext.getTenant();

        if (schema == null || schema.isBlank()) {
            throw new IllegalStateException("Tenant schema not set");
        }

        return schema;
    }
}