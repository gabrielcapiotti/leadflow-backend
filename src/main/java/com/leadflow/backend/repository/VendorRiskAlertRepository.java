package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorRiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VendorRiskAlertRepository
        extends JpaRepository<VendorRiskAlert, UUID> {

    boolean existsByVendorIdAndResolvedFalse(UUID vendorId);
}