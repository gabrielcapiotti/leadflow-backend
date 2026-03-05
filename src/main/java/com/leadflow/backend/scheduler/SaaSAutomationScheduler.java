package com.leadflow.backend.scheduler;

import com.leadflow.backend.service.audit.AuditCleanupService;
import com.leadflow.backend.service.vendor.SubscriptionService;
import com.leadflow.backend.service.vendor.VendorRiskService;
import com.leadflow.backend.service.vendor.VendorUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SaaSAutomationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SaaSAutomationScheduler.class);

    private final VendorUsageService usageService;
    private final SubscriptionService subscriptionService;
    private final VendorRiskService riskService;
    private final AuditCleanupService auditService;

    public SaaSAutomationScheduler(VendorUsageService usageService,
                                   SubscriptionService subscriptionService,
                                   VendorRiskService riskService,
                                   AuditCleanupService auditService) {

        this.usageService = usageService;
        this.subscriptionService = subscriptionService;
        this.riskService = riskService;
        this.auditService = auditService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void resetMonthlyUsage() {
        log.info("event=scheduler_reset_monthly_usage_start");
        usageService.resetMonthlyLimits();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkExpiredSubscriptions() {
        log.info("event=scheduler_check_expired_subscriptions_start");
        subscriptionService.expireSubscriptions();
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void checkUsageAlerts() {
        log.info("event=scheduler_check_usage_alerts_start");
        usageService.checkUsageAlerts();
    }

    @Scheduled(cron = "0 30 1 * * *")
    public void calculateVendorRisk() {
        log.info("event=scheduler_calculate_vendor_risk_start");
        riskService.analyzeVendors();
    }

    @Scheduled(cron = "0 0 3 * * MON")
    public void cleanupAuditLogs() {
        log.info("event=scheduler_cleanup_audit_logs_start");
        auditService.deleteOldLogs();
    }
}
