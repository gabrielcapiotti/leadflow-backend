package com.leadflow.backend.service.vendor;

import com.leadflow.backend.dto.vendor.DashboardResponse;
import com.leadflow.backend.dto.vendor.StageConversionResponse;
import com.leadflow.backend.dto.vendor.StageTimeMetricsResponse;
import com.leadflow.backend.entities.vendor.VendorLead;
import com.leadflow.backend.repository.VendorLeadRepository;
import com.leadflow.backend.security.VendorContext;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final VendorLeadRepository leadRepository;
    private final VendorLeadService leadService;
        private final VendorContext vendorContext;

    public DashboardService(VendorLeadRepository leadRepository,
                            VendorLeadService leadService,
                                                        VendorContext vendorContext) {
        this.leadRepository = leadRepository;
        this.leadService = leadService;
                this.vendorContext = vendorContext;
    }

        public DashboardResponse getDashboardForCurrentVendor() {
                UUID vendorId = vendorContext.getCurrentVendor().getId();

        List<VendorLead> leads =
                leadRepository.findByVendorId(vendorId);

        long total = leads.size();

        long hotLeads = leads.stream()
                .filter(l -> l.getScore() >= 85)
                .count();

        List<VendorLead> rankingTop5 =
                leads.stream()
                        .sorted((a, b) ->
                                Integer.compare(
                                        b.getScore(),
                                        a.getScore()))
                        .limit(5)
                        .toList();

        Map<String, Long> porStage =
                leads.stream()
                        .collect(Collectors.groupingBy(
                                l -> l.getStage().name(),
                                Collectors.counting()
                        ));

        StageConversionResponse conversao =
                leadService.calculateConversionRatesForCurrentVendor();

        StageTimeMetricsResponse tempo =
                leadService.calculateAverageStageTimeForCurrentVendor();

        return new DashboardResponse(
                rankingTop5,
                porStage,
                conversao.getConversionRates(),
                tempo.getAverageTimeInHours(),
                hotLeads,
                total
        );
    }
}
