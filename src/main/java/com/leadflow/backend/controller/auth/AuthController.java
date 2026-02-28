package com.leadflow.backend.controller.auth;

import com.leadflow.backend.dto.auth.*;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.security.CustomUserDetails;
import com.leadflow.backend.security.exception.UnauthorizedException;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.security.jwt.JwtToken;
import com.leadflow.backend.service.auth.AuthService;
import com.leadflow.backend.service.auth.RefreshTokenService;
import com.leadflow.backend.service.auth.UserSessionService;
import com.leadflow.domain.auth.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final UserSessionService userSessionService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PasswordResetService passwordResetService,
            UserSessionService userSessionService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetService = passwordResetService;
        this.userSessionService = userSessionService;
    }

    /* ======================================================
       REGISTER
       ====================================================== */

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {

        String tenant = requireTenant();
        UUID tenantId = resolveTenantId(tenant);

        User user = authService.registerUser(
                request.name(),
                request.email(),
                request.password()
        );

        JwtToken accessToken = jwtService.generateToken(user, tenant);

        createSession(user.getId(), tenantId, accessToken, httpRequest);

        String refreshToken = refreshTokenService.generate(
                user,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(
                        accessToken.getToken(),
                        refreshToken
                ));
    }

    /* ======================================================
       LOGIN
       ====================================================== */

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        String tenant = requireTenant();
        UUID tenantId = resolveTenantId(tenant);

        User user = authService.authenticateUser(
                request.email(),
                request.password()
        );

        JwtToken accessToken = jwtService.generateToken(user, tenant);

        createSession(user.getId(), tenantId, accessToken, httpRequest);

        String refreshToken = refreshTokenService.generate(
                user,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(
                new AuthResponse(
                        accessToken.getToken(),
                        refreshToken
                )
        );
    }

    /* ======================================================
       LIST ACTIVE SESSIONS
       ====================================================== */

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(
            Authentication authentication,
            HttpServletRequest request
    ) {

        CustomUserDetails user = requireAuthenticatedUser(authentication);

        String tenant = requireTenant();
        UUID tenantId = resolveTenantId(tenant);

        String token = extractToken(request);
        String tokenId = jwtService.extractTokenId(token);

        return ResponseEntity.ok(
                userSessionService.listActiveSessions(
                        user.getId(),
                        tenantId,
                        tokenId
                )
        );
    }

    /* ======================================================
       REVOKE SPECIFIC SESSION
       ====================================================== */

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable UUID sessionId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        CustomUserDetails user = requireAuthenticatedUser(authentication);

        String tenant = requireTenant();
        UUID tenantId = resolveTenantId(tenant);

        // Impede revogação da própria sessão atual
        String token = extractToken(request);
        String currentTokenId = jwtService.extractTokenId(token);

        List<SessionResponse> sessions =
                userSessionService.listActiveSessions(
                        user.getId(),
                        tenantId,
                        currentTokenId
                );

        sessions.stream()
                .filter(SessionResponse::current)
                .filter(s -> s.sessionId().equals(sessionId))
                .findAny()
                .ifPresent(s -> {
                    throw new IllegalArgumentException(
                            "Cannot revoke current active session"
                    );
                });

        userSessionService.revokeSpecificSession(
                sessionId,
                user.getId(),
                tenantId
        );

        return ResponseEntity.noContent().build();
    }

    /* ======================================================
       INTERNAL HELPERS
       ====================================================== */

    private CustomUserDetails requireAuthenticatedUser(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof CustomUserDetails user)) {

            throw new UnauthorizedException("Authentication required");
        }

        return user;
    }

    private void createSession(
            UUID userId,
            UUID tenantId,
            JwtToken accessToken,
            HttpServletRequest request
    ) {

        userSessionService.createSession(
                userId,
                tenantId,
                accessToken.getTokenId(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
    }

    private String requireTenant() {

        String schema = TenantContext.getTenant();

        if (schema == null || schema.isBlank()) {
            throw new UnauthorizedException("Tenant not resolved");
        }

        return schema;
    }

    private UUID resolveTenantId(String tenant) {

        try {
            return UUID.fromString(tenant);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid tenant identifier");
        }
    }

    private String extractToken(HttpServletRequest request) {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Missing or invalid Authorization header"
            );
        }

        return header.substring(7);
    }
}