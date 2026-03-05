package com.leadflow.backend.controller.auth;

import com.leadflow.backend.dto.auth.*;
import com.leadflow.backend.dto.user.UserResponse;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.CustomUserDetails;
import com.leadflow.backend.security.exception.UnauthorizedException;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.security.jwt.JwtToken;
import com.leadflow.backend.service.auth.AuthService;
import com.leadflow.backend.service.auth.RefreshTokenService;
import com.leadflow.backend.service.auth.UserSessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final UserSessionService userSessionService;
    private final TenantService tenantService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserSessionService userSessionService,
            TenantService tenantService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userSessionService = userSessionService;
        this.tenantService = tenantService;
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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse(accessToken.getToken(), refreshToken));
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
                new AuthResponse(accessToken.getToken(), refreshToken)
        );
    }

    /* ======================================================
       CURRENT USER
       ====================================================== */

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {

        CustomUserDetails userDetails = requireAuthenticatedUser(authentication);
        User user = userDetails.getUser();

        return ResponseEntity.ok(
                new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().getName()
                )
        );
    }

    /* ======================================================
       SESSIONS
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

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable UUID sessionId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        CustomUserDetails user = requireAuthenticatedUser(authentication);

        String tenant = requireTenant();
        UUID tenantId = resolveTenantId(tenant);

        userSessionService.revokeSpecificSession(
                sessionId,
                user.getId(),
                tenantId
        );

        return ResponseEntity.noContent().build();
    }

    /* ======================================================
       HELPERS
       ====================================================== */

    private CustomUserDetails requireAuthenticatedUser(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new UnauthorizedException("Authentication required");
        }

        return user;
    }

    private UUID resolveTenantId(String tenant) {

        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException("Tenant identifier is required");
        }

        UUID tenantId = tenantService.getTenantIdBySchema(tenant);

        if (tenantId == null) {
            throw new IllegalArgumentException("Invalid tenant");
        }

        return tenantId;
    }

    private void createSession(
            UUID userId,
            UUID tenantId,
            JwtToken accessToken,
            HttpServletRequest request
    ) {

        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("IP address cannot be null or blank");
        }

        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("User-Agent cannot be null or blank");
        }

        userSessionService.createSession(
                userId,
                tenantId,
                accessToken.getTokenId(),
                ipAddress,
                userAgent
        );
    }

    private String requireTenant() {
        return TenantContext.getTenant();
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