package com.leadflow.backend.controller.admin;

import com.leadflow.backend.dto.audit.SecurityAuditResponse;
import com.leadflow.backend.entities.audit.SecurityAction;
import com.leadflow.backend.entities.audit.SecurityAuditLog;
import com.leadflow.backend.repository.audit.SecurityAuditLogRepository;
import com.leadflow.backend.specification.SecurityAuditSpecification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private static final Logger logger =
            LoggerFactory.getLogger(AdminAuditController.class);

    private final SecurityAuditLogRepository repository;

    public AdminAuditController(SecurityAuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Consulta logs de auditoria de segurança com filtros opcionais.
     */
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
            Pageable pageable
    ) {

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

        Page<SecurityAuditResponse> response = repository
                .findAll(specification, pageable)
                .map(this::mapToResponse);

        logger.info("Admin audit query executed - filters: email={}, tenant={}, action={}, success={}",
                email, tenant, action, success);

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       MAPPING
       ====================================================== */

    private SecurityAuditResponse mapToResponse(SecurityAuditLog log) {
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

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid date range: 'from' must be before 'to'");
        }
    }
}