package com.leadflow.backend.service.audit;

import com.leadflow.backend.entities.system.AuditLog;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorAuditLog;
import com.leadflow.backend.repository.AuditLogRepository;
import com.leadflow.backend.repository.VendorAuditLogRepository;
import com.leadflow.backend.security.VendorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository systemRepository;
    private final VendorAuditLogRepository vendorRepository;
    private final VendorContext vendorContext;

    public AuditService(AuditLogRepository systemRepository,
                        VendorAuditLogRepository vendorRepository,
                        VendorContext vendorContext) {
        this.systemRepository = systemRepository;
        this.vendorRepository = vendorRepository;
        this.vendorContext = vendorContext;
    }

    /* ======================================================
       SYSTEM AUDIT
       ====================================================== */

    public void logSystem(String action,
                          String entityType,
                          String entityId,
                          String details) {

        try {

            String actor = resolveActor();

            AuditLog logEntry = new AuditLog();
            logEntry.setAction(action);
            logEntry.setEntityType(entityType);
            logEntry.setEntityId(entityId);
            logEntry.setActorEmail(actor);
            logEntry.setDetails(details);

            systemRepository.save(logEntry);

        } catch (Exception e) {
            log.error("Falha ao registrar auditoria de sistema", e);
        }
    }

    /* ======================================================
       VENDOR AUDIT
       ====================================================== */

    public void logVendor(String action,
                          String entityType,
                          UUID entityId,
                          String details) {

        try {

            Vendor vendor = vendorContext.getCurrentVendor();

            if (vendor == null) {
                logSystem(action, entityType,
                        entityId != null ? entityId.toString() : null,
                        details);
                return;
            }

            VendorAuditLog logEntry = new VendorAuditLog();
            logEntry.setVendorId(vendor.getId());
            logEntry.setUserEmail(vendor.getUserEmail());
            logEntry.setAcao(action);
            logEntry.setEntityType(entityType);
            logEntry.setEntidadeId(entityId);
            logEntry.setDetalhes(details);

            vendorRepository.save(logEntry);

        } catch (Exception e) {
            log.error("Falha ao registrar auditoria do vendor", e);
        }
    }

    /* ======================================================
       GENERIC LOG (used by AOP)
       ====================================================== */

    public void log(String action,
                    String entity,
                    String entityId,
                    String details) {

        UUID uuid = safeUUID(entityId);

        logVendor(action, entity, uuid, details);
    }

    /* ======================================================
       OVERLOADED GENERIC LOG
       ====================================================== */

    public void log(String action,
                    String entity,
                    UUID entityId,
                    String details) {

        logVendor(action, entity, entityId, details);
    }

    /* ======================================================
       HELPERS
       ====================================================== */

    private String resolveActor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return "system";
        }

        return authentication.getName();
    }

    private UUID safeUUID(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            log.warn("UUID inválido recebido para auditoria: {}", value);
            return null;
        }
    }
}