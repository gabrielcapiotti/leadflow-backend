package com.leadflow.backend.service.admin;

import com.leadflow.backend.dto.admin.GrowthPoint;
import com.leadflow.backend.dto.admin.GrowthResponse;
import com.leadflow.backend.dto.admin.AdminOverviewResponse;
import com.leadflow.backend.dto.admin.CohortResponse;
import com.leadflow.backend.dto.admin.ForecastPoint;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.dto.admin.VendorHealthResponse;
import com.leadflow.backend.entities.vendor.QuotaType;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.VendorRiskAlert;
import com.leadflow.backend.repository.SubscriptionHistoryRepository;
import com.leadflow.backend.repository.VendorLeadRepository;
import com.leadflow.backend.repository.VendorRiskAlertRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.repository.VendorUsageRepository;
import com.leadflow.backend.service.notification.SendGridEmailService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final double PLAN_PRICE = 197.0;
        private static final int DEFAULT_GROWTH_DAYS = 30;
        private static final int DEFAULT_FORECAST_MONTHS = 6;
        private static final int MAX_FORECAST_MONTHS = 36;

    private final VendorRepository vendorRepository;
    private final VendorLeadRepository leadRepository;
    private final VendorUsageRepository usageRepository;
        private final SubscriptionHistoryRepository historyRepository;
        private final VendorRiskAlertRepository riskAlertRepository;
        private final SendGridEmailService emailService;

        @Value("${app.risk-alert.admin-email:}")
        private String riskAlertAdminEmail;

    public AdminService(VendorRepository vendorRepository,
                        VendorLeadRepository leadRepository,
                                                VendorUsageRepository usageRepository,
                                                SubscriptionHistoryRepository historyRepository,
                                                VendorRiskAlertRepository riskAlertRepository,
                                                SendGridEmailService emailService) {
        this.vendorRepository = vendorRepository;
        this.leadRepository = leadRepository;
        this.usageRepository = usageRepository;
                this.historyRepository = historyRepository;
                this.riskAlertRepository = riskAlertRepository;
                this.emailService = emailService;
    }

    public AdminOverviewResponse getOverview() {

        long totalVendors = vendorRepository.countAllGlobal();

        long active = vendorRepository
                .countBySubscriptionStatusGlobal(SubscriptionStatus.ATIVA.name());

        long trial = vendorRepository
                .countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL.name());

        long inadimplentes = vendorRepository
                .countBySubscriptionStatusGlobal(SubscriptionStatus.INADIMPLENTE.name());

        long expiradas = vendorRepository
                .countBySubscriptionStatusGlobal(SubscriptionStatus.EXPIRADA.name());

        long totalLeads = leadRepository.countAllGlobal();

        long totalAi = usageRepository
                .sumUsedByQuotaTypeGlobal(QuotaType.AI_EXECUTIONS.name());

                double mrrReal = calculateMRR();
                double churnRate30d = calculateChurn(30);
                double trialToPaidConversion30d = calculateTrialConversion(30);
                double arpu = calculateARPU();
                double churnRate = calculateMonthlyChurn();
                double ltv = calculateLTV();

        return new AdminOverviewResponse(
                totalVendors,
                active,
                trial,
                inadimplentes,
                expiradas,
                totalLeads,
                totalAi,
                                mrrReal,
                                mrrReal,
                                churnRate30d,
                                trialToPaidConversion30d,
                                arpu,
                                churnRate,
                                ltv
        );
    }

        public double calculateMRR() {
                long active = vendorRepository.countActiveSubscriptionsGlobal();
                return active * PLAN_PRICE;
        }

        public double calculateARPU() {
                long active = vendorRepository.countActiveSubscriptionsGlobal();

                if (active == 0) {
                        return 0;
                }

                double revenue = calculateMRR();
                return revenue / active;
        }

        public double calculateMonthlyChurn() {

                Instant since = Instant.now()
                                .minus(30, ChronoUnit.DAYS);

                long cancellations = historyRepository.countCancellationsSinceGlobal(since);
                long activeBase = vendorRepository.countActiveSubscriptionsGlobal();

                if (activeBase == 0) {
                        return 0;
                }

                return cancellations / (double) activeBase;
        }

        @Cacheable("ltv")
        public double calculateLTV() {

                double arpu = calculateARPU();
                double churn = calculateMonthlyChurn();

                if (churn == 0) {
                        return arpu * 24;
                }

                return arpu / churn;
        }

        public double calculateChurn(int days) {

                int safeDays = days > 0 ? days : DEFAULT_GROWTH_DAYS;

                Instant since = Instant.now()
                                .minus(safeDays, ChronoUnit.DAYS);

                long cancellations = historyRepository.countCancellationsSinceGlobal(since);
                long activeBase = vendorRepository.countActiveSubscriptionsGlobal();

                if (activeBase == 0) {
                        return 0;
                }

                return cancellations / (double) activeBase;
        }

        public double calculateTrialConversion(int days) {

                int safeDays = days > 0 ? days : DEFAULT_GROWTH_DAYS;

                Instant since = Instant.now()
                                .minus(safeDays, ChronoUnit.DAYS);

                long conversions = historyRepository.countTrialConversionsSinceGlobal(since);
                long trials = vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL.name());

                if (trials == 0) {
                        return 0;
                }

                return conversions / (double) trials;
        }

    public GrowthResponse getGrowth(int days) {

        int safeDays = days > 0 ? days : DEFAULT_GROWTH_DAYS;

        Instant since = Instant.now()
                .minus(safeDays, ChronoUnit.DAYS);

        List<GrowthPoint> vendors = mapToPoints(
                vendorRepository.countVendorsPerDayGlobal(since)
        );

        List<GrowthPoint> leads = mapToPoints(
                leadRepository.countLeadsPerDayGlobal(since)
        );

        List<GrowthPoint> ai = mapToPoints(
                usageRepository.sumUsagePerDayGlobal(
                        QuotaType.AI_EXECUTIONS.name(),
                        since
                )
        );

        List<GrowthPoint> revenue = vendors.stream()
                .map(point -> new GrowthPoint(
                        point.getDate(),
                        Math.round(point.getValue() * PLAN_PRICE)
                ))
                .toList();

        return new GrowthResponse(vendors, revenue, leads, ai);
    }

        @Cacheable("cohorts")
        public List<CohortResponse> calculateCohorts() {

                List<Vendor> vendors = vendorRepository.findAllWithSubscriptionStart();

                Map<YearMonth, List<Vendor>> cohorts = vendors.stream()
                                .collect(Collectors.groupingBy(vendor ->
                                                YearMonth.from(vendor.getSubscriptionStartedAt().atZone(ZoneOffset.UTC))
                                ));

                List<CohortResponse> result = new ArrayList<>();

                cohorts.entrySet().stream()
                                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                                .forEach(entry -> {
                                        YearMonth cohortMonth = entry.getKey();
                                        List<Vendor> cohortVendors = entry.getValue();
                                        int total = cohortVendors.size();

                                        Map<Integer, Double> retentionMap = new LinkedHashMap<>();

                                        for (int monthOffset = 0; monthOffset <= 12; monthOffset++) {
                                                YearMonth checkMonth = cohortMonth.plusMonths(monthOffset);

                                                long activeCount = cohortVendors.stream()
                                                                .filter(vendor -> isActiveInMonth(vendor, checkMonth))
                                                                .count();

                                                double percentage = total == 0 ? 0 : (activeCount / (double) total) * 100;
                                                retentionMap.put(monthOffset, percentage);
                                        }

                                        result.add(new CohortResponse(cohortMonth.toString(), retentionMap));
                                });

                return result;
        }

        @Cacheable(value = "forecast", key = "#months")
        public List<ForecastPoint> forecastMRR(int months) {

                int safeMonths = months <= 0
                        ? DEFAULT_FORECAST_MONTHS
                        : Math.min(months, MAX_FORECAST_MONTHS);

                double churnRate = calculateMonthlyChurn();
                double conversionRate = calculateTrialConversion(30);

                long active = vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.ATIVA.name());
                long trials = vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL.name());

                YearMonth current = YearMonth.now(ZoneOffset.UTC);
                List<ForecastPoint> forecast = new ArrayList<>();

                for (int monthOffset = 1; monthOffset <= safeMonths; monthOffset++) {
                        long churned = Math.round(active * churnRate);
                        long converted = Math.round(trials * conversionRate);

                        active = Math.max(0, active + converted - churned);

                        double projectedMRR = active * PLAN_PRICE;

                        forecast.add(new ForecastPoint(
                                        current.plusMonths(monthOffset).toString(),
                                        projectedMRR
                        ));
                }

                return forecast;
        }

        public VendorHealthResponse calculateHealth(java.util.UUID vendorId) {

                Vendor vendor = vendorRepository.findById(vendorId)
                                .orElseThrow();

                double score = 0;
                score += scoreUsage(vendor) * 0.30;
                score += scoreLeads(vendor) * 0.25;
                score += scoreRecency(vendor) * 0.20;
                score += scoreSubscription(vendor) * 0.15;
                score += scorePaymentHistory(vendor) * 0.10;

                int finalScore = Math.min(100, (int) Math.round(score));

                String riskLevel = finalScore >= 75
                                ? "LOW"
                                : finalScore >= 50 ? "MEDIUM" : "HIGH";

                return new VendorHealthResponse(vendorId, finalScore, riskLevel);
        }

        public void evaluateRisk(java.util.UUID vendorId) {

                VendorHealthResponse health = calculateHealth(vendorId);

                boolean highRisk = "HIGH".equals(health.getRiskLevel()) || health.getScore() < 50;

                if (!highRisk) {
                        return;
                }

                boolean alreadyExists = riskAlertRepository
                                .existsByVendorIdAndResolvedFalse(vendorId);

                if (alreadyExists) {
                        return;
                }

                VendorRiskAlert alert = new VendorRiskAlert();
                alert.setVendorId(vendorId);
                alert.setScore(health.getScore());
                alert.setRiskLevel(health.getRiskLevel());

                riskAlertRepository.save(alert);

                notifyVendorAtRisk(vendorId, health);
        }

        public void evaluateAllVendorsRisk() {

                vendorRepository.findAll()
                                .forEach(vendor -> evaluateRisk(vendor.getId()));
        }

        private void notifyVendorAtRisk(java.util.UUID vendorId,
                                        VendorHealthResponse health) {

                Vendor vendor = vendorRepository.findById(vendorId)
                                .orElseThrow();

                String html = """
                                <h2>Estamos percebendo pouca atividade</h2>
                                <p>Seu uso do Leadflow AI caiu recentemente.</p>
                                <p>Health Score atual: <strong>%d</strong></p>
                                <p>Se precisar de ajuda, nossa equipe está pronta para apoiar.</p>
                                """.formatted(health.getScore());

                emailService.sendEmail(
                                vendor.getUserEmail(),
                                "Percebemos pouca atividade na sua conta",
                                html
                );

                if (riskAlertAdminEmail != null && !riskAlertAdminEmail.isBlank()) {
                        String adminHtml = """
                                        <h2>Vendor em risco detectado</h2>
                                        <p>Vendor ID: <strong>%s</strong></p>
                                        <p>E-mail: <strong>%s</strong></p>
                                        <p>Health Score: <strong>%d</strong></p>
                                        <p>Risk Level: <strong>%s</strong></p>
                                        """.formatted(vendor.getId(), vendor.getUserEmail(), health.getScore(), health.getRiskLevel());

                        emailService.sendEmail(
                                        riskAlertAdminEmail,
                                        "[Admin] Vendor com risco alto detectado",
                                        adminHtml
                        );
                }
        }

        private int scoreUsage(Vendor vendor) {

                Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

                long usageLast30 = usageRepository.sumLast30Days(
                                vendor.getId(),
                                QuotaType.AI_EXECUTIONS.name(),
                                since
                );

                if (usageLast30 > 500) {
                        return 100;
                }
                if (usageLast30 > 200) {
                        return 80;
                }
                if (usageLast30 > 50) {
                        return 50;
                }
                return 20;
        }

        private int scoreLeads(Vendor vendor) {

                Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
                long leads = leadRepository.countLast30Days(vendor.getId(), since);

                if (leads > 50) {
                        return 100;
                }
                if (leads > 20) {
                        return 70;
                }
                if (leads > 5) {
                        return 40;
                }
                return 15;
        }

        private int scoreRecency(Vendor vendor) {

                Instant lastActivity = usageRepository.lastActivity(vendor.getId());

                if (lastActivity == null) {
                        return 10;
                }

                long days = Duration.between(lastActivity, Instant.now()).toDays();

                if (days <= 3) {
                        return 100;
                }
                if (days <= 7) {
                        return 80;
                }
                if (days <= 14) {
                        return 50;
                }
                return 20;
        }

        private int scoreSubscription(Vendor vendor) {

                return switch (vendor.getSubscriptionStatus()) {
                        case ATIVA -> 100;
                        case TRIAL -> 70;
                        case INADIMPLENTE -> 20;
                        case EXPIRADA -> 10;
                        default -> 10;
                };
        }

        private int scorePaymentHistory(Vendor vendor) {
                return vendor.getSubscriptionStatus() == SubscriptionStatus.INADIMPLENTE ? 10 : 80;
        }

        private boolean isActiveInMonth(Vendor vendor, YearMonth month) {

                if (vendor.getSubscriptionStatus() != SubscriptionStatus.ATIVA) {
                        return false;
                }

                Instant start = vendor.getSubscriptionStartedAt();
                Instant end = vendor.getSubscriptionExpiresAt();

                if (start == null) {
                        return false;
                }

                YearMonth vendorStart = YearMonth.from(start.atZone(ZoneOffset.UTC));

                if (vendorStart.isAfter(month)) {
                        return false;
                }

                if (end != null) {
                        YearMonth vendorEnd = YearMonth.from(end.atZone(ZoneOffset.UTC));
                        return !vendorEnd.isBefore(month);
                }

                return true;
        }

    private List<GrowthPoint> mapToPoints(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new GrowthPoint(
                        toLocalDate(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    private LocalDate toLocalDate(Object rawDate) {

        if (rawDate instanceof LocalDate localDate) {
            return localDate;
        }

        if (rawDate instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        return LocalDate.parse(rawDate.toString());
    }
}
