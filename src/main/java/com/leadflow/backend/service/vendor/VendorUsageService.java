package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.QuotaType;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorUsage;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.repository.VendorUsageRepository;
import com.leadflow.backend.service.PlanService;
import com.leadflow.backend.service.notification.SendGridEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class VendorUsageService {

    private static final Logger log = LoggerFactory.getLogger(VendorUsageService.class);

    private final VendorUsageRepository repository;
    private final VendorRepository vendorRepository;
    private final SendGridEmailService emailService;
    private final PlanService planService;

    public VendorUsageService(VendorUsageRepository repository,
                              VendorRepository vendorRepository,
                              SendGridEmailService emailService,
                              PlanService planService) {
        this.repository = repository;
        this.vendorRepository = vendorRepository;
        this.emailService = emailService;
        this.planService = planService;
    }

    @Transactional
    public void resetMonthlyLimits() {

        Instant now = Instant.now();
        List<VendorUsage> usages = repository.findAll();

        for (VendorUsage usage : usages) {

            if (usage.getPeriodEnd() != null && usage.getPeriodEnd().isBefore(now)) {
                usage.setUsed(0);
                usage.setPeriodStart(now);
                usage.setPeriodEnd(now.plus(30, ChronoUnit.DAYS));
                usage.setAlert80Sent(false);
                usage.setAlert100Sent(false);
                repository.save(usage);
            }
        }

        log.info("event=usage_monthly_reset processed={}", usages.size());
    }

    @Transactional
    public void checkUsageAlerts() {

        List<VendorUsage> usages = repository.findAll();

        for (VendorUsage usage : usages) {
            int limit = getLimit(usage.getQuotaType());
            if (limit <= 0) {
                continue;
            }

            int used = usage.getUsed();
            int percent = (int) Math.floor((used / (double) limit) * 100);

            UUID safeVendorId = Objects.requireNonNull(usage.getVendorId());
            Vendor vendor = vendorRepository.findById(safeVendorId).orElse(null);
            if (vendor == null) {
                continue;
            }

            if (percent >= 80 && !usage.isAlert80Sent()) {
                emailService.sendEmail(
                        vendor.getUserEmail(),
                        "⚠ Você atingiu 80% do limite",
                        "Você atingiu " + percent + "% de uso em " + usage.getQuotaType() + "."
                );
                usage.setAlert80Sent(true);
            }

            if (percent >= 100 && !usage.isAlert100Sent()) {
                emailService.sendEmail(
                        vendor.getUserEmail(),
                        "🚫 Limite mensal atingido",
                        "Seu limite mensal de " + usage.getQuotaType() + " foi atingido."
                );
                usage.setAlert100Sent(true);
            }

            repository.save(usage);
        }

        log.info("event=usage_alerts_check processed={}", usages.size());
    }

    private int getLimit(QuotaType type) {
        return switch (type) {
            case ACTIVE_LEADS -> planService.getActivePlan().getMaxLeads();
            case AI_EXECUTIONS -> planService.getActivePlan().getMaxAiExecutions();
        };
    }
}
