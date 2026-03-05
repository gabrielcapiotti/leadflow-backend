package com.leadflow.backend.dto.admin;

public class AdminOverviewResponse {

    private final long total_vendors;
    private final long active_subscriptions;
    private final long trial_subscriptions;
    private final long inadimplentes;
    private final long expiradas;

    private final long total_leads;
    private final long total_ai_executions_current_cycle;

    private final double estimated_monthly_revenue;
    private final double mrr_real;
    private final double churn_rate_30d;
    private final double trial_to_paid_conversion_30d;
    private final double arpu;
    private final double churn_rate;
    private final double ltv;

    public AdminOverviewResponse(long totalVendors,
                                 long active,
                                 long trial,
                                 long inadimplentes,
                                 long expiradas,
                                 long totalLeads,
                                 long totalAi,
                                 double revenue,
                                 double mrrReal,
                                 double churnRate30d,
                                 double trialToPaidConversion30d,
                                 double arpu,
                                 double churnRate,
                                 double ltv) {

        this.total_vendors = totalVendors;
        this.active_subscriptions = active;
        this.trial_subscriptions = trial;
        this.inadimplentes = inadimplentes;
        this.expiradas = expiradas;
        this.total_leads = totalLeads;
        this.total_ai_executions_current_cycle = totalAi;
        this.estimated_monthly_revenue = revenue;
        this.mrr_real = mrrReal;
        this.churn_rate_30d = churnRate30d;
        this.trial_to_paid_conversion_30d = trialToPaidConversion30d;
        this.arpu = arpu;
        this.churn_rate = churnRate;
        this.ltv = ltv;
    }

    public long getTotal_vendors() {
        return total_vendors;
    }

    public long getActive_subscriptions() {
        return active_subscriptions;
    }

    public long getTrial_subscriptions() {
        return trial_subscriptions;
    }

    public long getInadimplentes() {
        return inadimplentes;
    }

    public long getExpiradas() {
        return expiradas;
    }

    public long getTotal_leads() {
        return total_leads;
    }

    public long getTotal_ai_executions_current_cycle() {
        return total_ai_executions_current_cycle;
    }

    public double getEstimated_monthly_revenue() {
        return estimated_monthly_revenue;
    }

    public double getMrr_real() {
        return mrr_real;
    }

    public double getChurn_rate_30d() {
        return churn_rate_30d;
    }

    public double getTrial_to_paid_conversion_30d() {
        return trial_to_paid_conversion_30d;
    }

    public double getArpu() {
        return arpu;
    }

    public double getChurn_rate() {
        return churn_rate;
    }

    public double getLtv() {
        return ltv;
    }
}
