package com.leadflow.backend.controller.auth;

import com.leadflow.backend.dto.auth.AuthResponse;
import com.leadflow.backend.dto.auth.LoginRequest;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.security.TokenService;
import com.leadflow.backend.service.auth.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    /* ==========================
       REGISTER
       ========================== */

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.registerUser(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );

        String tenant = "default_tenant"; // Substituir por lógica para obter o tenant
        String token = tokenService.generateToken(user, tenant);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse(token));
    }

    /* ==========================
       LOGIN
       ========================== */

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        User user = authService.authenticateUser(
                request.getEmail(),
                request.getPassword()
        );

        String tenant = "default_tenant"; // Substituir por lógica para obter o tenant
        String token = tokenService.generateToken(user, tenant);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    /* ==========================
       ME
       ========================== */

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authService.findByEmail(userDetails.getUsername());

        return ResponseEntity.ok(user);
    }
}
