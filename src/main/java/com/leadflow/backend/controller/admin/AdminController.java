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
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = Objects.requireNonNull(adminService);
    }

    /* ======================================================
       OVERVIEW
       ====================================================== */

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> overview() {

        AdminOverviewResponse response = adminService.getOverview();

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       GROWTH METRICS
       ====================================================== */

    @GetMapping("/metrics/growth")
    public ResponseEntity<GrowthResponse> growth(

            @RequestParam(defaultValue = "30")
            @Min(1)
            @Max(365)
            int days
    ) {

        GrowthResponse response = adminService.getGrowth(days);

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       COHORT ANALYSIS
       ====================================================== */

    @GetMapping("/metrics/cohorts")
    public ResponseEntity<List<CohortResponse>> cohorts() {

        List<CohortResponse> response = adminService.calculateCohorts();

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       MRR FORECAST
       ====================================================== */

    @GetMapping("/metrics/forecast")
    public ResponseEntity<List<ForecastPoint>> forecast(

            @RequestParam(defaultValue = "6")
            @Min(1)
            @Max(24)
            int months
    ) {

        List<ForecastPoint> response = adminService.forecastMRR(months);

        return ResponseEntity.ok(response);
    }

    /* ======================================================
       VENDOR HEALTH
       ====================================================== */

    @GetMapping("/metrics/health/{vendorId}")
    public ResponseEntity<VendorHealthResponse> health(

            @PathVariable
            @NotNull
            @NonNull
            UUID vendorId
    ) {

        UUID safeVendorId =
                Objects.requireNonNull(vendorId, "vendorId must not be null");

        VendorHealthResponse response =
                adminService.calculateHealth(safeVendorId);

        return ResponseEntity.ok(response);
    }
}