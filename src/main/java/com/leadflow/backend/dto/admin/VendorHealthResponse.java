package com.leadflow.backend.dto.admin;

import java.util.UUID;

public class VendorHealthResponse {

    private final UUID vendorId;
    private final int score;
    private final String riskLevel;

    public VendorHealthResponse(UUID vendorId, int score, String riskLevel) {
        this.vendorId = vendorId;
        this.score = score;
        this.riskLevel = riskLevel;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public int getScore() {
        return score;
    }

    public String getRiskLevel() {
        return riskLevel;
    }
}