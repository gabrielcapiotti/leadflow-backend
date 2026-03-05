package com.leadflow.backend.service.audit;

import com.leadflow.backend.repository.VendorAuditLogRepository;
import com.leadflow.backend.repository.audit.SecurityAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class AuditCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AuditCleanupService.class);

    private final VendorAuditLogRepository vendorAuditLogRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;

    @Value("${audit.cleanup.retention-days:90}")
    private int retentionDays;

    public AuditCleanupService(VendorAuditLogRepository vendorAuditLogRepository,
                               SecurityAuditLogRepository securityAuditLogRepository) {
        this.vendorAuditLogRepository = vendorAuditLogRepository;
        this.securityAuditLogRepository = securityAuditLogRepository;
    }

    @Transactional
    public void deleteOldLogs() {

        Instant vendorThreshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        LocalDateTime securityThreshold = LocalDateTime.ofInstant(vendorThreshold, ZoneOffset.UTC);

        long vendorDeleted = vendorAuditLogRepository.deleteByCreatedAtBefore(vendorThreshold);
        long securityDeleted = securityAuditLogRepository.deleteByCreatedAtBefore(securityThreshold);

        log.info("event=audit_cleanup vendorDeleted={} securityDeleted={} retentionDays={}",
                vendorDeleted, securityDeleted, retentionDays);
    }
}
