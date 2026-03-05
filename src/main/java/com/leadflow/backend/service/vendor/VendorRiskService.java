package com.leadflow.backend.service.vendor;

import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.service.admin.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VendorRiskService {

    private static final Logger log = LoggerFactory.getLogger(VendorRiskService.class);

    private final VendorRepository vendorRepository;
    private final AdminService adminService;

    public VendorRiskService(VendorRepository vendorRepository,
                             AdminService adminService) {
        this.vendorRepository = vendorRepository;
        this.adminService = adminService;
    }

    public void analyzeVendors() {
        vendorRepository.findAll().forEach(vendor -> adminService.evaluateRisk(vendor.getId()));
        log.info("event=vendor_risk_analysis processed={}", vendorRepository.count());
    }
}
