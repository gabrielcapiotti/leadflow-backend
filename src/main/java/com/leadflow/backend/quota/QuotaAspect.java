package com.leadflow.backend.quota;

import com.leadflow.backend.entities.vendor.QuotaType;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.vendor.QuotaService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Aspect
@Component
public class QuotaAspect {

    private final QuotaService quotaService;
    private final VendorContext vendorContext;

    public QuotaAspect(QuotaService quotaService, VendorContext vendorContext) {
        this.quotaService = quotaService;
        this.vendorContext = vendorContext;
    }

    @Around("@annotation(checkQuota)")
    public Object around(ProceedingJoinPoint joinPoint, CheckQuota checkQuota) throws Throwable {

        UUID vendorId = extractVendorId(joinPoint);
        QuotaType quotaType = mapQuotaType(checkQuota.type());

        quotaService.checkQuota(vendorId, quotaType);

        Object result = joinPoint.proceed();

        quotaService.increment(vendorId, quotaType);

        return result;
    }

    private UUID extractVendorId(ProceedingJoinPoint joinPoint) {

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UUID id) {
                return id;
            }
        }

        if (vendorContext.getCurrentVendor() != null && vendorContext.getCurrentVendor().getId() != null) {
            return vendorContext.getCurrentVendor().getId();
        }

        throw new RuntimeException("VendorId not found in method arguments");
    }

    private QuotaType mapQuotaType(String type) {

        if (type == null || type.isBlank()) {
            throw new RuntimeException("Quota type is required");
        }

        String normalized = type.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "IA_EXECUTION", "AI_EXECUTION", "AI_EXECUTIONS" -> QuotaType.AI_EXECUTIONS;
            case "LEAD_CREATION", "LEAD_CREATIONS", "ACTIVE_LEADS" -> QuotaType.ACTIVE_LEADS;
            default -> throw new RuntimeException("Unsupported quota type: " + type);
        };
    }
}
