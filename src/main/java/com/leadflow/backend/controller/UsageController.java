package com.leadflow.backend.controller;

import com.leadflow.backend.dto.vendor.UsageResponse;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.service.vendor.QuotaService;
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

    public UsageController(QuotaService quotaService,
                           VendorRepository vendorRepository) {
        this.quotaService = quotaService;
        this.vendorRepository = vendorRepository;
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
}
