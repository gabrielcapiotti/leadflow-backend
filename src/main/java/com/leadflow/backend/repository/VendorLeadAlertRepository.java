package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorLeadAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorLeadAlertRepository
        extends JpaRepository<VendorLeadAlert, UUID> {

    List<VendorLeadAlert> findByVendorLeadIdAndResolvidoFalseOrderByCreatedAtDesc(UUID vendorLeadId);
}
