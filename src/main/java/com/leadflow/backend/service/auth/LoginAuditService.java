package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.auth.LoginAudit;
import com.leadflow.backend.repository.auth.LoginAuditRepository;
import com.leadflow.backend.repository.tenant.TenantRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoginAuditService {

    private final LoginAuditRepository repository;
    private final TenantRepository tenantRepository;
    private final Clock clock;

    public LoginAuditService(LoginAuditRepository repository,
                             TenantRepository tenantRepository,
                             Clock clock) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.clock = clock;
    }

    /* ======================================================
       RECORD SUCCESS
       ====================================================== */

    @Transactional
    public void recordSuccess(UUID userId,
                              String tenantSchema,
                              String email,
                              String ip,
                              String userAgent,
                              boolean suspicious) {

        UUID tenantId = resolveTenantId(tenantSchema);

        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(email, "email cannot be null");

        LoginAudit audit = new LoginAudit();
        audit.setUserId(userId);
        audit.setTenantId(tenantId);
        audit.setEmail(normalizeEmail(email));
        audit.setIpAddress(ip);
        audit.setUserAgent(userAgent);
        audit.setSuccess(true);
        audit.setSuspicious(suspicious);
        audit.setCreatedAt(Instant.now(clock));

        repository.save(audit);
    }

    /* ======================================================
       RECORD FAILURE
       ====================================================== */

    @Transactional
    public void recordFailure(String tenantSchema,
                              String email,
                              String ip,
                              String userAgent,
                              String reason) {

        UUID tenantId = resolveTenantId(tenantSchema);

        Objects.requireNonNull(email, "email cannot be null");

        LoginAudit audit = new LoginAudit();
        audit.setTenantId(tenantId);
        audit.setEmail(normalizeEmail(email));
        audit.setIpAddress(ip);
        audit.setUserAgent(userAgent);
        audit.setSuccess(false);
        audit.setFailureReason(reason);
        audit.setSuspicious(false);
        audit.setCreatedAt(Instant.now(clock));

        repository.save(audit);
    }

    /* ======================================================
       BRUTE FORCE DETECTION
       ====================================================== */

    @Transactional(readOnly = true)
    public boolean isBruteForceDetected(String email,
                                        String tenantSchema,
                                        int maxAttempts,
                                        int windowMinutes) {

        if (email == null || email.isBlank())
            return false;

        if (maxAttempts <= 0 || windowMinutes <= 0)
            return false;

        UUID tenantId = resolveTenantId(tenantSchema);

        Instant threshold = Instant.now(clock)
                .minusSeconds(windowMinutes * 60L);

        long failedAttempts =
                repository.countByEmailAndTenantIdAndSuccessFalseAndCreatedAtAfter(
                        normalizeEmail(email),
                        tenantId,
                        threshold
                );

        return failedAttempts >= maxAttempts;
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private UUID resolveTenantId(String tenantSchema) {

        if (tenantSchema == null || tenantSchema.isBlank()) {
            throw new IllegalStateException("Tenant schema cannot be null or blank");
        }

        return tenantRepository
                .findBySchemaNameIgnoreCaseAndDeletedAtIsNull(tenantSchema.trim())
                .map(Tenant::getId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Tenant not found: " + tenantSchema
                        )
                );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}