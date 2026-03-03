package com.leadflow.backend.dto.vendor;

import com.leadflow.backend.entities.vendor.VendorLead;

import java.util.List;
import java.util.Map;

public record DashboardResponse(

        List<VendorLead> rankingTop5,

        Map<String, Long> leadsPorStage,

        Map<String, Double> taxaConversao,

        Map<String, Double> tempoMedioPorStage,

        long leadsQuentes,

        long totalLeads
) {}
