package com.leadflow.backend.controller;

import com.leadflow.backend.dto.vendor.UsageResponse;
import com.leadflow.backend.entities.UsageLimit;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.vendor.QuotaService;
import com.leadflow.backend.service.vendor.UsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usage")
public class UsageController {

    private final QuotaService quotaService;
    private final VendorRepository vendorRepository;
    private final UsageService usageService;
    private final VendorContext vendorContext;

    public UsageController(QuotaService quotaService,
                           VendorRepository vendorRepository,
                           UsageService usageService,
                           VendorContext vendorContext) {
        this.quotaService = quotaService;
        this.vendorRepository = vendorRepository;
        this.usageService = usageService;
        this.vendorContext = vendorContext;
    }

    @GetMapping
    @PreAuthorize("@subscriptionGuard.isActive()")
    public UsageResponse getUsage() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Vendor vendor = vendorRepository
                .findByUserEmail(email)
                .stream()
                .findFirst()
                .orElseThrow();

        return quotaService.getUsage(vendor.getId());
    }

    @GetMapping("/limits")
    @PreAuthorize("@subscriptionGuard.isActive()")
    public ResponseEntity<UsageLimit> getUsageLimits() {
        UsageLimit usage = usageService.getUsage(vendorContext.getCurrentVendorId());
        return ResponseEntity.ok(usage);
    }
}
