package com.leadflow.backend.service.vendor;

import com.leadflow.backend.dto.vendor.UsageResponse;
import com.leadflow.backend.entities.vendor.QuotaType;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorUsage;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.repository.VendorUsageRepository;
import com.leadflow.backend.service.notification.SendGridEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final VendorUsageRepository repository;
    private final VendorRepository vendorRepository;
    private final SendGridEmailService emailService;
    private int maxActiveLeads = 500;
    private int maxAiExecutions = 1000;

    public QuotaService(VendorUsageRepository repository,
                        VendorRepository vendorRepository,
                        SendGridEmailService emailService) {
        this.repository = repository;
        this.vendorRepository = vendorRepository;
        this.emailService = emailService;
    }

    @Value("${subscription.limits.active-leads:500}")
    void setMaxActiveLeads(int maxActiveLeads) {
        if (maxActiveLeads > 0) {
            this.maxActiveLeads = maxActiveLeads;
        }
    }

    @Value("${subscription.limits.ai-executions:1000}")
    void setMaxAiExecutions(int maxAiExecutions) {
        if (maxAiExecutions > 0) {
            this.maxAiExecutions = maxAiExecutions;
        }
    }

    public void checkQuota(UUID vendorId, QuotaType type) {

        VendorUsage usage = getOrCreateUsage(vendorId, type);

        int limit = getLimit(type);

        if (usage.getUsed() >= limit) {
            throw new IllegalStateException(
                    "Limite mensal atingido para " + type
            );
        }
    }

    public void increment(UUID vendorId, QuotaType type) {

        VendorUsage usage = getOrCreateUsage(vendorId, type);

        usage.setUsed(usage.getUsed() + 1);

        int limit = getLimit(type);
        double percentage = (usage.getUsed() / (double) limit) * 100;

        if (percentage >= 80 && !usage.isAlert80Sent()) {
            handle80PercentAlert(vendorId, type, usage);
            usage.setAlert80Sent(true);
        }

        if (percentage >= 100 && !usage.isAlert100Sent()) {
            handle100PercentAlert(vendorId, type, usage);
            usage.setAlert100Sent(true);
        }

        repository.save(usage);
    }

        public UsageResponse getUsage(UUID vendorId) {

        VendorUsage leadsUsage =
            getOrCreateUsage(vendorId, QuotaType.ACTIVE_LEADS);

        VendorUsage aiUsage =
            getOrCreateUsage(vendorId, QuotaType.AI_EXECUTIONS);

        UsageResponse.ResourceUsage leads =
            new UsageResponse.ResourceUsage(
                leadsUsage.getUsed(), maxActiveLeads
            );

        UsageResponse.ResourceUsage ai =
            new UsageResponse.ResourceUsage(
                aiUsage.getUsed(), maxAiExecutions
            );

        return new UsageResponse(
            leads,
            ai,
            leadsUsage.getPeriodEnd()
        );
        }

    private int getLimit(QuotaType type) {

        return switch (type) {
            case ACTIVE_LEADS -> maxActiveLeads;
            case AI_EXECUTIONS -> maxAiExecutions;
        };
    }

    private VendorUsage getOrCreateUsage(UUID vendorId, QuotaType type) {

        Instant now = Instant.now();

        VendorUsage usage = repository
                .findByVendorIdAndQuotaType(vendorId, type)
                .orElseGet(() -> createNewPeriod(vendorId, type));

        if (usage.getPeriodEnd().isBefore(now)) {
            usage.setUsed(0);
            usage.setAlert80Sent(false);
            usage.setAlert100Sent(false);
            usage.setPeriodStart(now);
            usage.setPeriodEnd(now.plusSeconds(30L * 86400L));
            usage = repository.save(usage);
        }

        return usage;
    }

    private VendorUsage createNewPeriod(UUID vendorId, QuotaType type) {

        Instant now = Instant.now();

        VendorUsage usage = new VendorUsage();
        usage.setVendorId(vendorId);
        usage.setQuotaType(type);
        usage.setPeriodStart(now);
        usage.setPeriodEnd(now.plusSeconds(30L * 86400L));
        usage.setUsed(0);
        usage.setAlert80Sent(false);
        usage.setAlert100Sent(false);

        return repository.save(usage);
    }

    private void handle80PercentAlert(UUID vendorId, QuotaType type, VendorUsage usage) {
        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();

        int limit = getLimit(type);
        int percentage = (usage.getUsed() * 100) / limit;

        emailService.sendEmail(
            vendor.getUserEmail(),
            "⚠ Você atingiu 80% do limite",
            buildUsageWarningTemplate(type.name(), usage.getUsed(), limit, percentage)
        );

        log.info("Alerta de 80% enviado para vendor {} no recurso {}", vendorId, type);
    }

    private void handle100PercentAlert(UUID vendorId, QuotaType type, VendorUsage usage) {
        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();

        int limit = getLimit(type);

        emailService.sendEmail(
            vendor.getUserEmail(),
            "🚫 Limite mensal atingido",
            buildLimitReachedTemplate(type.name(), limit)
        );

        log.info("Alerta de 100% enviado para vendor {} no recurso {}", vendorId, type);
    }

    private String buildUsageWarningTemplate(String resource,
                                             int used,
                                             int limit,
                                             int percentage) {

        return """
                <html>
                <body style="font-family: Arial; background:#f4f4f4; padding:20px;">
                    <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px;">
                        <h2 style="color:#8B0000;">⚠ Atenção ao seu uso</h2>

                        <p>Você já utilizou:</p>

                        <h1 style="color:#8B0000;">%d%% do seu limite</h1>

                        <p><strong>%d</strong> de <strong>%d</strong> (%s)</p>

                        <div style="background:#eee; height:20px; border-radius:10px;">
                            <div style="width:%d%%; background:#8B0000; height:100%%; border-radius:10px;"></div>
                        </div>

                        <p style="margin-top:20px;">
                            Ao atingir 100%% o recurso será temporariamente bloqueado.
                        </p>

                        <a href="https://seusite.com/checkout"
                             style="display:inline-block; margin-top:20px; padding:12px 20px;
                             background:#8B0000; color:white; text-decoration:none;
                             border-radius:5px;">
                            Gerenciar Assinatura
                        </a>

                        <p style="margin-top:30px; font-size:12px; color:#888;">
                            Leadflow AI
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(percentage, used, limit, resource, percentage);
    }

    private String buildLimitReachedTemplate(String resource,
                                             int limit) {

        return """
                <html>
                <body style="font-family: Arial; background:#f4f4f4; padding:20px;">
                    <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px;">
                        <h2 style="color:#8B0000;">🚫 Limite atingido</h2>

                        <p>Você atingiu o limite mensal de:</p>

                        <h1 style="color:#8B0000;">%d (%s)</h1>

                        <p>O recurso foi temporariamente bloqueado até o próximo ciclo.</p>

                        <a href="https://seusite.com/checkout"
                             style="display:inline-block; margin-top:20px; padding:12px 20px;
                             background:#8B0000; color:white; text-decoration:none;
                             border-radius:5px;">
                            Atualizar Plano
                        </a>

                        <p style="margin-top:30px; font-size:12px; color:#888;">
                            Leadflow AI
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(limit, resource);
    }
}
