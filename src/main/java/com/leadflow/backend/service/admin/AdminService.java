package com.leadflow.backend.service.admin;

import com.leadflow.backend.dto.admin.*;
import com.leadflow.backend.entities.vendor.*;
import com.leadflow.backend.repository.*;
import com.leadflow.backend.service.notification.SendGridEmailService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

    public AdminService(
            VendorRepository vendorRepository,
            VendorLeadRepository leadRepository,
            VendorUsageRepository usageRepository,
            SubscriptionHistoryRepository historyRepository,
            VendorRiskAlertRepository riskAlertRepository,
            SendGridEmailService emailService
    ) {
        this.vendorRepository = Objects.requireNonNull(vendorRepository);
        this.leadRepository = Objects.requireNonNull(leadRepository);
        this.usageRepository = Objects.requireNonNull(usageRepository);
        this.historyRepository = Objects.requireNonNull(historyRepository);
        this.riskAlertRepository = Objects.requireNonNull(riskAlertRepository);
        this.emailService = Objects.requireNonNull(emailService);
    }

    /* ======================================================
       OVERVIEW
       ====================================================== */

    public AdminOverviewResponse getOverview() {

        long totalVendors = vendorRepository.countAllGlobal();
        long active = vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.ATIVA);
        long trial = vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL);
        long overdue = vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.INADIMPLENTE);
        long expired = vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.EXPIRADA);

        long totalLeads = leadRepository.countAllGlobal();

        long totalAi = Optional.ofNullable(
                usageRepository.sumUsedByQuotaTypeGlobal(QuotaType.AI_EXECUTIONS.name())
        ).orElse(0L);

        double mrr = calculateMRR();
        double churnRate30d = calculateChurn(30);
        double trialConversion = calculateTrialConversion(30);
        double arpu = calculateARPU();
        double churnRate = calculateMonthlyChurn();
        double ltv = calculateLTV();

        return new AdminOverviewResponse(
                totalVendors,
                active,
                trial,
                overdue,
                expired,
                totalLeads,
                totalAi,
                BigDecimal.valueOf(mrr),
                BigDecimal.valueOf(mrr),
                churnRate30d,
                trialConversion,
                BigDecimal.valueOf(arpu),
                churnRate,
                BigDecimal.valueOf(ltv)
        );
    }

    /* ======================================================
       FINANCIAL METRICS
       ====================================================== */

    public double calculateMRR() {
        long active = vendorRepository.countActiveSubscriptionsGlobal();
        return active * PLAN_PRICE;
    }

    public double calculateARPU() {

        long active = vendorRepository.countActiveSubscriptionsGlobal();
        if (active == 0) {
            return 0;
        }

        return calculateMRR() / active;
    }

    public double calculateMonthlyChurn() {

        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

        long cancellations = historyRepository.countCancellationsSinceGlobal(since);
        long activeBase = vendorRepository.countActiveSubscriptionsGlobal();

        if (activeBase == 0) {
            return 0;
        }

        return cancellations / (double) activeBase;
    }

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

        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);

        long cancellations = historyRepository.countCancellationsSinceGlobal(since);
        long activeBase = vendorRepository.countActiveSubscriptionsGlobal();

        if (activeBase == 0) {
            return 0;
        }

        return cancellations / (double) activeBase;
    }

    public double calculateTrialConversion(int days) {

        int safeDays = days > 0 ? days : DEFAULT_GROWTH_DAYS;

        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);

        long conversions = historyRepository.countTrialConversionsSinceGlobal(since);
        long trials = vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL);

        if (trials == 0) {
            return 0;
        }

        return conversions / (double) trials;
    }

    /* ======================================================
       GROWTH
       ====================================================== */

    public GrowthResponse getGrowth(int days) {

        int safeDays = days > 0 ? days : DEFAULT_GROWTH_DAYS;
        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);

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
                .map(p -> new GrowthPoint(
                        p.getDate(),
                        Math.round(p.getValue() * PLAN_PRICE)
                ))
                .toList();

        return new GrowthResponse(vendors, revenue, leads, ai);
    }

    /* ======================================================
       COHORT
       ====================================================== */

    public List<CohortResponse> calculateCohorts() {

        List<Vendor> vendors = vendorRepository.findAllWithSubscriptionStart();

        Map<YearMonth, List<Vendor>> cohorts =
                vendors.stream()
                        .collect(Collectors.groupingBy(v ->
                                YearMonth.from(
                                        v.getSubscriptionStartedAt()
                                                .atZone(ZoneOffset.UTC)
                                )
                        ));

        List<CohortResponse> result = new ArrayList<>();

        cohorts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {

                    YearMonth cohortMonth = entry.getKey();
                    List<Vendor> cohortVendors = entry.getValue();
                    int total = cohortVendors.size();

                    Map<Integer, Double> retention = new LinkedHashMap<>();

                    for (int month = 0; month <= 12; month++) {

                        YearMonth checkMonth = cohortMonth.plusMonths(month);

                        long active =
                                cohortVendors.stream()
                                        .filter(v -> isActiveInMonth(v, checkMonth))
                                        .count();

                        double percentage =
                                total == 0 ? 0 : (active / (double) total) * 100;

                        retention.put(month, percentage);
                    }

                    result.add(new CohortResponse(cohortMonth.toString(), retention));
                });

        return result;
    }

    /* ======================================================
       FORECAST
       ====================================================== */

    public List<ForecastPoint> forecastMRR(int months) {

        int safeMonths =
                months <= 0
                        ? DEFAULT_FORECAST_MONTHS
                        : Math.min(months, MAX_FORECAST_MONTHS);

        double churnRate = calculateMonthlyChurn();
        double conversionRate = calculateTrialConversion(30);

        long active =
                vendorRepository.countBySubscriptionStatusGlobal(
                        SubscriptionStatus.ATIVA
                );

        long trials =
                vendorRepository.countBySubscriptionStatusGlobal(
                        SubscriptionStatus.TRIAL
                );

        YearMonth current = YearMonth.now(ZoneOffset.UTC);

        List<ForecastPoint> forecast = new ArrayList<>();

        for (int month = 1; month <= safeMonths; month++) {

            long churned = Math.round(active * churnRate);
            long converted = Math.round(trials * conversionRate);

            active = Math.max(0, active + converted - churned);

            double projectedMRR = active * PLAN_PRICE;

            forecast.add(
                    new ForecastPoint(
                            current.plusMonths(month).toString(),
                            projectedMRR
                    )
            );
        }

        return forecast;
    }

    /* ======================================================
       HEALTH SCORE
       ====================================================== */

    public VendorHealthResponse calculateHealth(@NonNull UUID vendorId) {

        UUID safeVendorId = Objects.requireNonNull(vendorId);

        Vendor vendor = vendorRepository.findById(safeVendorId)
                .orElseThrow(() -> new RuntimeException("Vendor não encontrado"));

        double score =
                scoreUsage(vendor) * 0.30 +
                scoreLeads(vendor) * 0.25 +
                scoreRecency(vendor) * 0.20 +
                scoreSubscription(vendor) * 0.15 +
                scorePaymentHistory(vendor) * 0.10;

        int finalScore = Math.min(100, (int) Math.round(score));

        String riskLevel =
                finalScore >= 75 ? "LOW"
                        : finalScore >= 50 ? "MEDIUM"
                        : "HIGH";

        return new VendorHealthResponse(safeVendorId, finalScore, riskLevel);
    }

    public void evaluateRisk(@NonNull UUID vendorId) {

        UUID safeVendorId = Objects.requireNonNull(vendorId);

        VendorHealthResponse health = calculateHealth(safeVendorId);

        boolean highRisk =
                "HIGH".equals(health.getRiskLevel()) ||
                health.getScore() < 50;

        if (!highRisk) {
            return;
        }

        boolean exists =
                riskAlertRepository.existsByVendorIdAndResolvedFalse(safeVendorId);

        if (exists) {
            return;
        }

        VendorRiskAlert alert = new VendorRiskAlert();
        alert.setVendorId(safeVendorId);
        alert.setScore(health.getScore());
        alert.setRiskLevel(health.getRiskLevel());

        riskAlertRepository.save(alert);

        notifyVendorAtRisk(safeVendorId, health);
    }

    public void evaluateAllVendorsRisk() {

        vendorRepository.findAll()
                                .stream()
                                .map(Vendor::getId)
                                .filter(Objects::nonNull)
                                .forEach(this::evaluateRisk);
    }

    public void evaluateAllVendorsRiskDaily() {
        vendorRepository.findAll()
            .forEach(vendor -> evaluateRisk(vendor.getId()));
    }

    private void notifyVendorAtRisk(UUID vendorId,
                                    VendorHealthResponse health) {

        UUID safeVendorId = Objects.requireNonNull(vendorId);

        Vendor vendor = vendorRepository.findById(safeVendorId)
                .orElseThrow(() -> new RuntimeException("Vendor não encontrado"));

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
    }

    /* ======================================================
       SCORE COMPONENTS
       ====================================================== */

    private int scoreUsage(Vendor vendor) {

        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

        long usage = Optional.ofNullable(
                usageRepository.sumLast30Days(
                        vendor.getId(),
                        QuotaType.AI_EXECUTIONS.name(),
                        since
                )
        ).orElse(0L);

        if (usage > 500) return 100;
        if (usage > 200) return 80;
        if (usage > 50) return 50;

        return 20;
    }

    private int scoreLeads(Vendor vendor) {

        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

        long leads = leadRepository.countLast30Days(vendor.getId(), since);

        if (leads > 50) return 100;
        if (leads > 20) return 70;
        if (leads > 5) return 40;

        return 15;
    }

    private int scoreRecency(Vendor vendor) {

        Instant last = usageRepository.lastActivity(vendor.getId());

        if (last == null) return 10;

        long days = Duration.between(last, Instant.now()).toDays();

        if (days <= 3) return 100;
        if (days <= 7) return 80;
        if (days <= 14) return 50;

        return 20;
    }

    private int scoreSubscription(Vendor vendor) {

        SubscriptionStatus status = vendor.getSubscriptionStatus();

        if (status == null) {
            return 0;
        }

        return switch (status) {
            case ATIVA -> 100;
            case TRIAL -> 70;
            case INADIMPLENTE -> 20;
            case EXPIRADA -> 10;
            default -> 0;
        };
    }

    private int scorePaymentHistory(Vendor vendor) {

        return vendor.getSubscriptionStatus() ==
                SubscriptionStatus.INADIMPLENTE ? 10 : 80;
    }

    /* ======================================================
       HELPERS
       ====================================================== */

    private boolean isActiveInMonth(Vendor vendor, YearMonth month) {

        if (vendor.getSubscriptionStatus() != SubscriptionStatus.ATIVA) {
            return false;
        }

        Instant start = vendor.getSubscriptionStartedAt();
        Instant end = vendor.getSubscriptionExpiresAt();

        if (start == null) return false;

        YearMonth vendorStart =
                YearMonth.from(start.atZone(ZoneOffset.UTC));

        if (vendorStart.isAfter(month)) return false;

        if (end != null) {

            YearMonth vendorEnd =
                    YearMonth.from(end.atZone(ZoneOffset.UTC));

            return !vendorEnd.isBefore(month);
        }

        return true;
    }

    private List<GrowthPoint> mapToPoints(List<Object[]> rows) {

        return rows.stream()
                .map(r -> new GrowthPoint(
                        toLocalDate(r[0]),
                        ((Number) r[1]).longValue()
                ))
                .toList();
    }

    private LocalDate toLocalDate(Object rawDate) {

        if (rawDate instanceof LocalDate d) return d;

        if (rawDate instanceof Date d) return d.toLocalDate();

        return LocalDate.parse(rawDate.toString());
    }
}