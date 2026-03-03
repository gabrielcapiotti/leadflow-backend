package com.leadflow.backend.dto.vendor;

import com.leadflow.backend.entities.vendor.LeadStage;

public class UpdateStageRequest {

    private LeadStage stage;

    public LeadStage getStage() {
        return stage;
    }

    public void setStage(LeadStage stage) {
        this.stage = stage;
    }
}