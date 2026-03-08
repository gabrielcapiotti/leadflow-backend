package com.leadflow.backend.controller.admin;

import com.leadflow.backend.dto.audit.SecurityAuditResponse;
import com.leadflow.backend.dto.audit.VendorAuditResponse;
import com.leadflow.backend.entities.audit.SecurityAction;
import com.leadflow.backend.entities.audit.SecurityAuditLog;
import com.leadflow.backend.entities.vendor.VendorAuditLog;
import com.leadflow.backend.repository.VendorAuditLogRepository;
import com.leadflow.backend.repository.audit.SecurityAuditLogRepository;
import com.leadflow.backend.specification.SecurityAuditSpecification;
import com.leadflow.backend.specification.VendorAuditSpecification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private static final Logger logger =
            LoggerFactory.getLogger(AdminAuditController.class);

    private final SecurityAuditLogRepository securityAuditLogRepository;
    private final VendorAuditLogRepository vendorAuditLogRepository;

    public AdminAuditController(
            SecurityAuditLogRepository securityAuditLogRepository,
            VendorAuditLogRepository vendorAuditLogRepository
    ) {
        this.securityAuditLogRepository = securityAuditLogRepository;
        this.vendorAuditLogRepository = vendorAuditLogRepository;
    }

    /* ======================================================
       SECURITY AUDIT
       ====================================================== */

    @GetMapping("/security")
    public ResponseEntity<Page<SecurityAuditResponse>> getAuditLogs(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String tenant,
            @RequestParam(required = false) SecurityAction action,
            @RequestParam(required = false) Boolean success,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @NonNull Pageable pageable
    ) {

        Pageable safePageable =
                Objects.requireNonNull(pageable, "Pageable must not be null");

        validateDateRange(from, to);

        Specification<SecurityAuditLog> specification =
                SecurityAuditSpecification.filter(
                        email,
                        tenant,
                        action,
                        success,
                        from,
                        to
                );

        Page<SecurityAuditResponse> response =
                securityAuditLogRepository
                        .findAll(specification, safePageable)
                        .map(this::mapSecurityAuditResponse);

        logger.info(
                "Admin security audit query executed - email={}, tenant={}, action={}, success={}",
                email, tenant, action, success
        );

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       VENDOR AUDIT
       ====================================================== */

    @GetMapping("/vendor")
    public ResponseEntity<Page<VendorAuditResponse>> getVendorAuditLogs(

            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String entityType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @NonNull Pageable pageable
    ) {

        Pageable safePageable =
                Objects.requireNonNull(pageable, "Pageable must not be null");

        validateDateRange(from, to);

        Specification<VendorAuditLog> specification =
                VendorAuditSpecification.filter(
                        vendorId,
                        acao,
                        entityType,
                        from,
                        to
                );

        Page<VendorAuditResponse> response =
                vendorAuditLogRepository
                        .findAll(specification, safePageable)
                        .map(this::mapVendorAuditResponse);

        logger.info(
                "Admin vendor audit query executed - vendorId={}, acao={}, entityType={}",
                vendorId, acao, entityType
        );

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       MAPPING
       ====================================================== */

    private SecurityAuditResponse mapSecurityAuditResponse(SecurityAuditLog log) {

        return new SecurityAuditResponse(
                log.getId(),
                log.getAction(),
                log.getEmail(),
                log.getTenant(),
                log.isSuccess(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCorrelationId(),
                log.getCreatedAt()
        );
    }

    private VendorAuditResponse mapVendorAuditResponse(VendorAuditLog log) {

        return new VendorAuditResponse(
                log.getId(),
                log.getVendorId(),
                log.getUserEmail(),
                log.getAcao(),
                log.getEntityType(),
                log.getEntidadeId(),
                log.getDetalhes(),
                log.getCreatedAt()
        );
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Invalid date range: 'from' must be before 'to'"
            );
        }
    }

    private void validateDateRange(Instant from, Instant to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Invalid date range: 'from' must be before 'to'"
            );
        }
    }
}