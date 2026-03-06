package com.leadflow.backend.service.vendor;

import java.util.UUID;

import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorAuditLog;
import com.leadflow.backend.repository.VendorAuditLogRepository;
import com.leadflow.backend.security.VendorContext;

public class VendorAuditService {

    private final VendorAuditLogRepository repository;
    private final VendorContext vendorContext;

    public VendorAuditService(VendorAuditLogRepository repository, VendorContext vendorContext) {
        this.repository = repository;
        this.vendorContext = vendorContext;
    }

    public void logAction(String action, String entity, String entityId, String details) {
        Vendor vendor = vendorContext.getCurrentVendor();
        repository.save(new VendorAuditLog(
            vendor.getId(),
            vendor.getUserEmail(), // Updated to use getUserEmail()
            action,
            entity,
            UUID.fromString(entityId),
            details
        ));
    }
}