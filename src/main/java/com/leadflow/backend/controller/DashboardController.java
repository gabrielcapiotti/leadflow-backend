package com.leadflow.backend.controller;

import com.leadflow.backend.dto.vendor.DashboardResponse;
import com.leadflow.backend.service.vendor.DashboardService;

import org.springframework.web.bind.annotation.*;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboardForCurrentVendor();
    }
}
