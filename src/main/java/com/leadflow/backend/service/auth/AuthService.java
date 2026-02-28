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

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService auditService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            SecurityAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /* ======================================================
       REGISTER
       ====================================================== */

    @Transactional
    public User registerUser(String name, String email, String password) {

        validateInput(name, email, password);

        String schema = resolveCurrentTenantSchema();
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

        logger.info("User registered in schema {} with email {}",
                schema, normalizedEmail);

        return user;
    }

    /* ======================================================
       LOGIN (COM BLOQUEIO + AUDITORIA)
       ====================================================== */

    @Transactional
    public User authenticateUser(String email, String password) {

        if (email == null || email.isBlank() ||
            password == null || password.isBlank()) {

            audit(SecurityAction.LOGIN_FAILED, email, false);
            throw new IllegalArgumentException("Invalid credentials");
        }

        String schema = resolveCurrentTenantSchema();
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> {
                    audit(SecurityAction.LOGIN_FAILED, normalizedEmail, false);
                    return new IllegalArgumentException("Invalid credentials");
                });

        if (user.isAccountLocked()) {

            audit(SecurityAction.ACCOUNT_LOCKED, normalizedEmail, false);

            logger.warn("Blocked login attempt for user {} in schema {}",
                    normalizedEmail, schema);

            throw new IllegalStateException("Account temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {

            user.registerFailedLogin();
            userRepository.save(user);

            audit(SecurityAction.LOGIN_FAILED, normalizedEmail, false);

            logger.warn("Failed login attempt for user {} in schema {}",
                    normalizedEmail, schema);

            throw new IllegalArgumentException("Invalid credentials");
        }

        user.resetLoginAttempts();
        userRepository.save(user);

        audit(SecurityAction.LOGIN_SUCCESS, normalizedEmail, true);

        logger.info("User authenticated in schema {}: {}",
                schema, normalizedEmail);

        return user;
    }

    /* ======================================================
       FIND BY EMAIL
       ====================================================== */

    @Transactional(readOnly = true)
    public User findByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        String normalizedEmail = normalizeEmail(email);

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    /* ======================================================
       AUDIT INTERNAL
       ====================================================== */

    private void audit(SecurityAction action, String email, boolean success) {

        String tenant = resolveCurrentTenantSchema();
        HttpServletRequest request = currentRequest();

        String ip = request != null ? request.getRemoteAddr() : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        String correlationId = MDC.get("correlationId");

        auditService.log(
                action,
                email,
                tenant,
                success,
                ip,
                userAgent,
                correlationId
        );
    }

    private HttpServletRequest currentRequest() {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes != null ? attributes.getRequest() : null;
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

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
            throw new IllegalArgumentException("Password must contain at least 8 characters");
    }
}