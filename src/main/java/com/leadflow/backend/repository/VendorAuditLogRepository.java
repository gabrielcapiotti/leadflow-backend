package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorAuditLogRepository
        extends JpaRepository<VendorAuditLog, UUID> {

    List<VendorAuditLog> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
}
