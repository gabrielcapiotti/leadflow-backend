package com.leadflow.backend.controller.admin;

import com.leadflow.backend.dto.admin.AdminOverviewResponse;
import com.leadflow.backend.dto.admin.CohortResponse;
import com.leadflow.backend.dto.admin.ForecastPoint;
import com.leadflow.backend.dto.admin.GrowthResponse;
import com.leadflow.backend.dto.admin.VendorHealthResponse;
import com.leadflow.backend.service.admin.AdminService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /* ======================================================
       OVERVIEW
       ====================================================== */

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> overview() {
        return ResponseEntity.ok(adminService.getOverview());
    }

    /* ======================================================
       GROWTH METRICS
       ====================================================== */

    @GetMapping("/metrics/growth")
    public ResponseEntity<GrowthResponse> growth(
            @RequestParam(defaultValue = "30")
            @Min(1) @Max(365)
            int days
    ) {
        return ResponseEntity.ok(adminService.getGrowth(days));
    }

    /* ======================================================
       COHORT ANALYSIS
       ====================================================== */

    @GetMapping("/metrics/cohorts")
    public ResponseEntity<List<CohortResponse>> cohorts() {
        return ResponseEntity.ok(adminService.calculateCohorts());
    }

    /* ======================================================
       MRR FORECAST
       ====================================================== */

    @GetMapping("/metrics/forecast")
    public ResponseEntity<List<ForecastPoint>> forecast(
            @RequestParam(defaultValue = "6")
            @Min(1) @Max(24)
            int months
    ) {
        return ResponseEntity.ok(adminService.forecastMRR(months));
    }

    /* ======================================================
       VENDOR HEALTH
       ====================================================== */

    @GetMapping("/metrics/health/{vendorId}")
    public ResponseEntity<VendorHealthResponse> health(
            @PathVariable
            @NotNull
            UUID vendorId
    ) {
        return ResponseEntity.ok(adminService.calculateHealth(vendorId));
    }
}