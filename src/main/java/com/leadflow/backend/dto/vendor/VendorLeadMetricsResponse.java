package com.leadflow.backend.dto.vendor;

import java.util.Map;

public class VendorLeadMetricsResponse {

    private Map<String, Long> stages;

    public VendorLeadMetricsResponse(Map<String, Long> stages) {
        this.stages = stages;
    }

    public Map<String, Long> getStages() {
        return stages;
    }
}