package com.leadflow.backend.controller;

import com.leadflow.backend.dto.vendor.DashboardResponse;
import com.leadflow.backend.service.vendor.DashboardService;
import com.leadflow.backend.security.SubscriptionGuard;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("@subscriptionGuard.isActive()")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SubscriptionGuard subscriptionGuard;

    public DashboardController(
            DashboardService dashboardService,
            SubscriptionGuard subscriptionGuard
    ) {
        this.dashboardService = dashboardService;
        this.subscriptionGuard = subscriptionGuard;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        subscriptionGuard.assertActive();
        return dashboardService.getDashboardForCurrentVendor();
    }
}
