package com.leadflow.backend.service.vendor;

import com.leadflow.backend.dto.admin.VendorHealthResponse;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.service.admin.AdminService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class VendorRiskService {

    private static final Logger log =
            LoggerFactory.getLogger(VendorRiskService.class);

    private final VendorRepository vendorRepository;
    private final AdminService adminService;

    public VendorRiskService(
            VendorRepository vendorRepository,
            AdminService adminService
    ) {
        this.vendorRepository = Objects.requireNonNull(vendorRepository);
        this.adminService = Objects.requireNonNull(adminService);
    }

    public void analyzeVendors() {

        long processed = 0;

        for (Vendor vendor : vendorRepository.findAll()) {

            UUID vendorId =
                    Objects.requireNonNull(
                            vendor.getId(),
                            "Vendor ID must not be null"
                    );

            VendorHealthResponse health =
                    adminService.calculateHealth(vendorId);

            if ("HIGH".equals(health.getRiskLevel())) {

                log.warn(
                        "event=vendor_risk_detected vendorId={} score={}",
                        vendorId,
                        health.getScore()
                );
            }

            processed++;
        }

        log.info(
                "event=vendor_risk_analysis processed={}",
                processed
        );
    }
}