package com.leadflow.backend.dto.admin;

import java.util.Map;

public class CohortResponse {

    private final String cohort;
    private final Map<Integer, Double> retention;

    public CohortResponse(String cohort, Map<Integer, Double> retention) {
        this.cohort = cohort;
        this.retention = retention;
    }

    public String getCohort() {
        return cohort;
    }

    public Map<Integer, Double> getRetention() {
        return retention;
    }
}