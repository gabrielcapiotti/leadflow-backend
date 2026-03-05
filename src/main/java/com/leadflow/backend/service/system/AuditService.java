package com.leadflow.backend.service.system;

import com.leadflow.backend.entities.system.AuditLog;
import com.leadflow.backend.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("systemAuditService")
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String action, String entity, String entityId, String details) {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String actor = "system";
        if (authentication != null && authentication.getName() != null) {
            actor = authentication.getName();
        }

        AuditLog log = new AuditLog();

        log.setAction(action);
        log.setEntityType(entity);
        log.setEntityId(entityId);
        log.setActorEmail(actor);
        log.setDetails(details);

        repository.save(log);
    }
}
