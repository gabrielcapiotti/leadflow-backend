package com.leadflow.backend.audit;

import com.leadflow.backend.service.system.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {

        Object result = joinPoint.proceed();

        String entityId = extractEntityId(joinPoint.getArgs(), result);

        auditService.log(
                audit.action(),
                audit.entity(),
                entityId,
                "method=" + joinPoint.getSignature().getName()
        );

        return result;
    }

    private String extractEntityId(Object[] args, Object result) {
        for (Object arg : args) {
            if (arg instanceof UUID id) {
                return id.toString();
            }
        }

        if (result == null) {
            return null;
        }

        try {
            Object id = result.getClass().getMethod("getId").invoke(result);
            if (id != null) {
                return id.toString();
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }

        return null;
    }
}
