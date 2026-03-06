package com.leadflow.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class AdminOverviewResponse {

    private final long totalVendors;
    private final long activeSubscriptions;
    private final long trialSubscriptions;
    private final long overdueSubscriptions;
    private final long expiredSubscriptions;

    private final long totalLeads;
    private final long totalAiExecutionsCurrentCycle;

    private final BigDecimal estimatedMonthlyRevenue;
    private final BigDecimal mrrReal;

    private final double churnRate30d;
    private final double trialToPaidConversion30d;

    private final BigDecimal arpu;
    private final double churnRate;
    private final BigDecimal ltv;

    public AdminOverviewResponse(
            long totalVendors,
            long activeSubscriptions,
            long trialSubscriptions,
            long overdueSubscriptions,
            long expiredSubscriptions,
            long totalLeads,
            long totalAiExecutionsCurrentCycle,
            BigDecimal estimatedMonthlyRevenue,
            BigDecimal mrrReal,
            double churnRate30d,
            double trialToPaidConversion30d,
            BigDecimal arpu,
            double churnRate,
            BigDecimal ltv
    ) {
        this.totalVendors = totalVendors;
        this.activeSubscriptions = activeSubscriptions;
        this.trialSubscriptions = trialSubscriptions;
        this.overdueSubscriptions = overdueSubscriptions;
        this.expiredSubscriptions = expiredSubscriptions;
        this.totalLeads = totalLeads;
        this.totalAiExecutionsCurrentCycle = totalAiExecutionsCurrentCycle;
        this.estimatedMonthlyRevenue = estimatedMonthlyRevenue;
        this.mrrReal = mrrReal;
        this.churnRate30d = churnRate30d;
        this.trialToPaidConversion30d = trialToPaidConversion30d;
        this.arpu = arpu;
        this.churnRate = churnRate;
        this.ltv = ltv;
    }

    @JsonProperty("total_vendors")
    public long getTotalVendors() {
        return totalVendors;
    }

    @JsonProperty("active_subscriptions")
    public long getActiveSubscriptions() {
        return activeSubscriptions;
    }

    @JsonProperty("trial_subscriptions")
    public long getTrialSubscriptions() {
        return trialSubscriptions;
    }

    @JsonProperty("inadimplentes")
    public long getOverdueSubscriptions() {
        return overdueSubscriptions;
    }

    @JsonProperty("expiradas")
    public long getExpiredSubscriptions() {
        return expiredSubscriptions;
    }

    @JsonProperty("total_leads")
    public long getTotalLeads() {
        return totalLeads;
    }

    @JsonProperty("total_ai_executions_current_cycle")
    public long getTotalAiExecutionsCurrentCycle() {
        return totalAiExecutionsCurrentCycle;
    }

    @JsonProperty("estimated_monthly_revenue")
    public BigDecimal getEstimatedMonthlyRevenue() {
        return estimatedMonthlyRevenue;
    }

    @JsonProperty("mrr_real")
    public BigDecimal getMrrReal() {
        return mrrReal;
    }

    @JsonProperty("churn_rate_30d")
    public double getChurnRate30d() {
        return churnRate30d;
    }

    @JsonProperty("trial_to_paid_conversion_30d")
    public double getTrialToPaidConversion30d() {
        return trialToPaidConversion30d;
    }

    @JsonProperty("arpu")
    public BigDecimal getArpu() {
        return arpu;
    }

    @JsonProperty("churn_rate")
    public double getChurnRate() {
        return churnRate;
    }

    @JsonProperty("ltv")
    public BigDecimal getLtv() {
        return ltv;
    }
}