package com.leadflow.backend.dto.admin;

public class ForecastPoint {

    private final String month;
    private final double projected_mrr;

    public ForecastPoint(String month, double projected_mrr) {
        this.month = month;
        this.projected_mrr = projected_mrr;
    }

    public String getMonth() {
        return month;
    }

    public double getProjected_mrr() {
        return projected_mrr;
    }
}