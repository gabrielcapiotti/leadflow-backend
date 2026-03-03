package com.leadflow.backend.service.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter leadsCreated;
    private final Counter hotLeads;
    private final Counter aiCalls;

    public MetricsService(MeterRegistry registry) {

        this.leadsCreated = registry.counter("lead.created");
        this.hotLeads = registry.counter("lead.hot");
        this.aiCalls = registry.counter("ai.calls");
    }

    public void incrementLeadCreated() {
        leadsCreated.increment();
    }

    public void incrementHotLead() {
        hotLeads.increment();
    }

    public void incrementAiCalls() {
        aiCalls.increment();
    }
}
