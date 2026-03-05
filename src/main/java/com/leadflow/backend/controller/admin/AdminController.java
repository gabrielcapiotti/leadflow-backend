package com.leadflow.backend.controller.admin;

import com.leadflow.backend.dto.admin.AdminOverviewResponse;
import com.leadflow.backend.dto.admin.CohortResponse;
import com.leadflow.backend.dto.admin.ForecastPoint;
import com.leadflow.backend.dto.admin.GrowthResponse;
import com.leadflow.backend.dto.admin.VendorHealthResponse;
import com.leadflow.backend.service.admin.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return adminService.getOverview();
    }

    @GetMapping("/metrics/growth")
    public GrowthResponse growth(@RequestParam(defaultValue = "30") int days) {
        return adminService.getGrowth(days);
    }

    @GetMapping("/metrics/cohorts")
    public List<CohortResponse> cohorts() {
        return adminService.calculateCohorts();
    }

    @GetMapping("/metrics/forecast")
    public List<ForecastPoint> forecast(@RequestParam(defaultValue = "6") int months) {
        return adminService.forecastMRR(months);
    }

    @GetMapping("/metrics/health/{vendorId}")
    public VendorHealthResponse health(@PathVariable UUID vendorId) {
        return adminService.calculateHealth(vendorId);
    }
}
