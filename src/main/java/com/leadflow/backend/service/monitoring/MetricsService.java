package com.leadflow.backend.service.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    private final Counter leadsCreatedTotal;
    private final Counter hotLeadsTotal;
    private final Counter aiExecutionsTotal;

    /*
     * Cache de counters por vendor para evitar recriação constante
     */
    private final Map<String, Counter> vendorLeadCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> vendorAiCounters = new ConcurrentHashMap<>();

    public MetricsService(MeterRegistry registry) {

        this.registry = registry;

        this.leadsCreatedTotal =
                registry.counter("lead.created.total");

        this.hotLeadsTotal =
                registry.counter("lead.hot.total");

        this.aiExecutionsTotal =
                registry.counter("ai.executions.total");
    }

    /*
     * ------------------------------
     * LEADS
     * ------------------------------
     */

    public void incrementLeadCreated() {
        leadsCreatedTotal.increment();
    }

    public void incrementLeadCreated(String vendorId) {

        vendorLeadCounters
                .computeIfAbsent(vendorId, id ->
                        registry.counter(
                                "lead.created.total",
                                "vendor", id
                        ))
                .increment();
    }

    public void incrementHotLead() {
        hotLeadsTotal.increment();
    }

    /*
     * ------------------------------
     * AI
     * ------------------------------
     */

    public void incrementAiExecutions() {
        aiExecutionsTotal.increment();
    }

    public void incrementAiExecutions(String vendorId) {

        vendorAiCounters
                .computeIfAbsent(vendorId, id ->
                        registry.counter(
                                "ai.executions.total",
                                "vendor", id
                        ))
                .increment();
    }
}