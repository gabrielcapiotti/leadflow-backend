package com.leadflow.backend.service.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry registry;
    private final Counter leadsCreated;
    private final Counter hotLeads;
    private final Counter aiCalls;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;

        this.leadsCreated = Counter.builder("lead.created")
                .description("Total leads created")
                .register(registry);

        this.hotLeads = Counter.builder("lead.hot")
                .description("Total hot leads")
                .register(registry);

        this.aiCalls = Counter.builder("ai.executions")
                .description("Total AI executions")
                .register(registry);
    }

    public void incrementLeadCreated() {
        leadsCreated.increment();
    }

    public void leadCreated() {
        incrementLeadCreated();
    }

    public void leadCreated(String vendorId) {
        Counter.builder("lead.created")
                .description("Total leads created")
                .tag("vendor", vendorId)
                .register(registry)
                .increment();
    }

    public void incrementHotLead() {
        hotLeads.increment();
    }

    public void incrementAiCalls() {
        aiCalls.increment();
    }

    public void aiExecution() {
        incrementAiCalls();
    }
}
