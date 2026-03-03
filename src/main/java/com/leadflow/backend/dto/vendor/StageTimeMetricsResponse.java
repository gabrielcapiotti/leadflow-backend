package com.leadflow.backend.dto.vendor;

import java.util.Map;

public class StageTimeMetricsResponse {

    private Map<String, Double> averageTimeInHours;

    public StageTimeMetricsResponse(Map<String, Double> averageTimeInHours) {
        this.averageTimeInHours = averageTimeInHours;
    }

    public Map<String, Double> getAverageTimeInHours() {
        return averageTimeInHours;
    }
}