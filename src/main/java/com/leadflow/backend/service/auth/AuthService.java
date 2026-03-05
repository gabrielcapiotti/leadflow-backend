package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.audit.SecurityAction;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.service.audit.SecurityAuditService;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    /* ====================================================== */
    /* REGISTER                                               */
    /* ====================================================== */

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

    /* ====================================================== */
    /* AUTHENTICATE                                           */
    /* ====================================================== */

    @Transactional
    public User authenticateUser(String email, String password) {

        HttpServletRequest request = currentRequest();

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {

            recordFailureAudit(email, "Invalid credentials");
            throw new IllegalArgumentException("Invalid credentials");
        }

        String normalizedEmail = normalizeEmail(email);
        String tenantSchema = TenantContext.getTenant();
        String ip = request != null ? request.getRemoteAddr() : "unknown";

        String emailKey = "bf:email:" + tenantSchema + ":" + normalizedEmail;
        String ipKey = "bf:ip:" + tenantSchema + ":" + ip;

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

            bruteForceService.recordFailure(emailKey, windowMinutes);
            bruteForceService.recordFailure(ipKey, windowMinutes);

            recordFailureAudit(normalizedEmail, "Wrong password");

            logger.warn("Invalid password attempt for {}", normalizedEmail);

            throw new IllegalArgumentException("Invalid credentials");
        }

        user.resetLoginAttempts();
        userRepository.save(user);

        bruteForceService.reset(emailKey);
        bruteForceService.reset(ipKey);

        recordSuccessAudit(user);

        logger.info("User authenticated successfully: {}", normalizedEmail);

        return user;
    }

    /* ====================================================== */
    /* AUDIT HELPERS                                          */
    /* ====================================================== */

    private void recordSuccessAudit(User user) {

        HttpServletRequest request = currentRequest();

        loginAuditService.recordSuccess(
                user.getId(),
                TenantContext.getTenant(),
                user.getEmail(),
                request != null ? request.getRemoteAddr() : null,
                request != null ? request.getHeader("User-Agent") : null,
                false
        );

        audit(SecurityAction.LOGIN_SUCCESS, user.getEmail(), true);
    }

    private void recordFailureAudit(String email, String reason) {

        HttpServletRequest request = currentRequest();

        loginAuditService.recordFailure(
                TenantContext.getTenant(),
                email,
                request != null ? request.getRemoteAddr() : null,
                request != null ? request.getHeader("User-Agent") : null,
                reason
        );

        audit(SecurityAction.LOGIN_FAILED, email, false);
    }

    private void audit(SecurityAction action, String email, boolean success) {

        HttpServletRequest request = currentRequest();

        auditService.log(
                action,
                email,
                TenantContext.getTenant(),
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