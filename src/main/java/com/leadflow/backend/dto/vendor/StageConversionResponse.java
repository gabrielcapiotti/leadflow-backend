package com.leadflow.backend.dto.vendor;

import java.util.Map;

public class StageConversionResponse {

    private Map<String, Double> conversionRates;

    public StageConversionResponse(Map<String, Double> conversionRates) {
        this.conversionRates = conversionRates;
    }

    public Map<String, Double> getConversionRates() {
        return conversionRates;
    }
}