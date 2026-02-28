package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.audit.SecurityAction;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.service.audit.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService auditService;
    private final LoginAuditService loginAuditService;
    private final BruteForceProtectionService bruteForceService;

    private final int maxAttempts;
    private final int windowMinutes;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            SecurityAuditService auditService,
            LoginAuditService loginAuditService,
            BruteForceProtectionService bruteForceService,
            @Value("${security.brute-force.max-attempts:5}") int maxAttempts,
            @Value("${security.brute-force.window-minutes:5}") int windowMinutes
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.loginAuditService = loginAuditService;
        this.bruteForceService = bruteForceService;
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.windowMinutes = Math.max(windowMinutes, 1);
    }

    /* ======================================================
       REGISTER
       ====================================================== */

    @Transactional
    public User registerUser(String name, String email, String password) {

        validateInput(name, email, password);

        String normalizedEmail = normalizeEmail(email);

        if (userRepository
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {

            audit(SecurityAction.USER_REGISTERED, normalizedEmail, false);
            throw new IllegalArgumentException("Email already in use");
        }

        Role userRole = roleRepository
                .findByNameIgnoreCase("ROLE_USER")
                .orElseThrow(() ->
                        new IllegalStateException("Default role ROLE_USER not found")
                );

        User user = new User(
                name.trim(),
                normalizedEmail,
                passwordEncoder.encode(password),
                userRole
        );

        userRepository.save(user);

        audit(SecurityAction.USER_REGISTERED, normalizedEmail, true);

        logger.info("User registered successfully: {}", normalizedEmail);

        return user;
    }

    /* ======================================================
       LOGIN WITH REDIS BRUTE FORCE PROTECTION
       ====================================================== */

    @Transactional
    public User authenticateUser(String email, String password) {

        HttpServletRequest request = currentRequest(); // ✅ CORREÇÃO

        if (email == null || email.isBlank() ||
            password == null || password.isBlank()) {

            recordFailureAudit(email, "Invalid credentials");
            throw new IllegalArgumentException("Invalid credentials");
        }

        String normalizedEmail = normalizeEmail(email);
        UUID tenantId = resolveTenantId();

        String ip = request != null ? request.getRemoteAddr() : "unknown";

        String emailKey = "bf:email:" + tenantId + ":" + normalizedEmail;
        String ipKey = "bf:ip:" + tenantId + ":" + ip;

        // 🔴 Verificação de bloqueio
        if (bruteForceService.isBlocked(emailKey, maxAttempts)
                || bruteForceService.isBlocked(ipKey, maxAttempts)) {

            recordFailureAudit(normalizedEmail, "Brute force detected");

            logger.warn("Brute-force blocked for email {}", normalizedEmail);

            throw new IllegalStateException(
                    "Too many failed attempts. Try again later."
            );
        }

        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> {
                    recordFailureAudit(normalizedEmail, "User not found");
                    return new IllegalArgumentException("Invalid credentials");
                });

        if (user.isAccountLocked()) {

            recordFailureAudit(normalizedEmail, "Account locked");

            logger.warn("Blocked login for locked account: {}", normalizedEmail);

            throw new IllegalStateException(
                    "Account temporarily locked. Try again later."
            );
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {

            user.registerFailedLogin();
            userRepository.save(user);

            // 🔴 Incrementa Redis
            bruteForceService.recordFailure(emailKey, windowMinutes);
            bruteForceService.recordFailure(ipKey, windowMinutes);

            recordFailureAudit(normalizedEmail, "Wrong password");

            logger.warn("Invalid password attempt for {}", normalizedEmail);

            throw new IllegalArgumentException("Invalid credentials");
        }

        // ✅ Login bem-sucedido
        user.resetLoginAttempts();
        userRepository.save(user);

        // 🔵 Reset Redis
        bruteForceService.reset(emailKey);
        bruteForceService.reset(ipKey);

        recordSuccessAudit(user);

        audit(SecurityAction.LOGIN_SUCCESS, normalizedEmail, true);

        logger.info("User authenticated successfully: {}", normalizedEmail);

        return user;
    }

    /* ======================================================
       FIND USER
       ====================================================== */

    @Transactional(readOnly = true)
    public User findByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(email))
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    /* ======================================================
       LOGIN AUDIT HELPERS
       ====================================================== */

    private void recordSuccessAudit(User user) {

        HttpServletRequest request = currentRequest();

        loginAuditService.recordSuccess(
                user.getId(),
                resolveTenantId(),
                user.getEmail(),
                request != null ? request.getRemoteAddr() : null,
                request != null ? request.getHeader("User-Agent") : null,
                false
        );
    }

    private void recordFailureAudit(String email, String reason) {

        HttpServletRequest request = currentRequest();

        loginAuditService.recordFailure(
                resolveTenantId(),
                email,
                request != null ? request.getRemoteAddr() : null,
                request != null ? request.getHeader("User-Agent") : null,
                reason
        );
    }

    /* ======================================================
       SECURITY AUDIT
       ====================================================== */

    private void audit(SecurityAction action, String email, boolean success) {

        HttpServletRequest request = currentRequest();

        auditService.log(
                action,
                email,
                resolveCurrentTenantSchema(),
                success,
                request != null ? request.getRemoteAddr() : null,
                request != null ? request.getHeader("User-Agent") : null,
                MDC.get("correlationId")
        );
    }

    private HttpServletRequest currentRequest() {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes != null ? attributes.getRequest() : null;
    }

    /* ======================================================
       INTERNAL HELPERS
       ====================================================== */

    private UUID resolveTenantId() {

        String tenant = TenantContext.getTenant();

        if (tenant == null || tenant.isBlank()) {
            throw new IllegalStateException("No tenant defined in TenantContext");
        }

        return UUID.fromString(tenant);
    }

    private String resolveCurrentTenantSchema() {

        String schema = TenantContext.getTenant();

        if (schema == null || schema.isBlank()) {
            throw new IllegalStateException("No tenant defined in TenantContext");
        }

        return schema.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private void validateInput(String name, String email, String password) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank");

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be blank");

        if (password == null || password.length() < 8)
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters"
            );
    }
}