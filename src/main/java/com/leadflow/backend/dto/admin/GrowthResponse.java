package com.leadflow.backend.dto.admin;

import java.util.List;

public class GrowthResponse {

    private final List<GrowthPoint> vendors;
    private final List<GrowthPoint> revenue;
    private final List<GrowthPoint> leads;
    private final List<GrowthPoint> ai_executions;

    public GrowthResponse(List<GrowthPoint> vendors,
                          List<GrowthPoint> revenue,
                          List<GrowthPoint> leads,
                          List<GrowthPoint> aiExecutions) {
        this.vendors = vendors;
        this.revenue = revenue;
        this.leads = leads;
        this.ai_executions = aiExecutions;
    }

    public List<GrowthPoint> getVendors() {
        return vendors;
    }

    public List<GrowthPoint> getRevenue() {
        return revenue;
    }

    public List<GrowthPoint> getLeads() {
        return leads;
    }

    public List<GrowthPoint> getAi_executions() {
        return ai_executions;
    }
}
