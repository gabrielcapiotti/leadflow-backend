package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VendorAuditLogRepository
    extends JpaRepository<VendorAuditLog, UUID>,
        JpaSpecificationExecutor<VendorAuditLog> {

    List<VendorAuditLog> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    long deleteByCreatedAtBefore(Instant threshold);
}
