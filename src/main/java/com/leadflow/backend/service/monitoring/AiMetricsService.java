package com.leadflow.backend.service.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class AiMetricsService {

    private final Counter aiExecutions;

    public AiMetricsService(MeterRegistry registry) {
        this.aiExecutions = Counter.builder("leadflow.ai.executions")
                .description("Total AI executions")
                .register(registry);
    }

    public void increment() {
        aiExecutions.increment();
    }
}
