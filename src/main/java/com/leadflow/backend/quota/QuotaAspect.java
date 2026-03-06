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

    public QuotaAspect(
            QuotaService quotaService,
            VendorContext vendorContext
    ) {
        this.quotaService = quotaService;
        this.vendorContext = vendorContext;
    }

    @Around("@annotation(checkQuota)")
    public Object around(ProceedingJoinPoint joinPoint, CheckQuota checkQuota) throws Throwable {

        UUID vendorId = resolveVendorId();
        QuotaType quotaType = resolveQuotaType(checkQuota.type());

        // Valida limite antes da execução
        quotaService.checkQuota(vendorId, quotaType);

        Object result;

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // Não incrementa quota se operação falhar
            throw ex;
        }

        // Incrementa quota apenas após execução bem-sucedida
        quotaService.increment(vendorId, quotaType);

        return result;
    }

    private UUID resolveVendorId() {

        if (vendorContext == null || vendorContext.getCurrentVendor() == null) {
            throw new IllegalStateException("Vendor context not available");
        }

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        if (vendorId == null) {
            throw new IllegalStateException("Vendor ID not available in VendorContext");
        }

        return vendorId;
    }

    private QuotaType resolveQuotaType(String type) {

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Quota type must be provided");
        }

        String normalized = type.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "AI_EXECUTION", "AI_EXECUTIONS", "IA_EXECUTION" -> QuotaType.AI_EXECUTIONS;
            case "LEAD_CREATION", "LEAD_CREATIONS", "ACTIVE_LEADS" -> QuotaType.ACTIVE_LEADS;
            default -> throw new IllegalArgumentException("Unsupported quota type: " + type);
        };
    }
}