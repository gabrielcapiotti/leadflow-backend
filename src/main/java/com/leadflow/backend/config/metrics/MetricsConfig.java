package com.leadflow.backend.config.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MetricsConfig {

    /*
     * Tags comuns para todas as métricas
     */
    @Bean
    public MeterFilter commonTags() {
        return MeterFilter.commonTags(
                List.of(
                        Tag.of("application", "leadflow-backend")
                )
        );
    }

    /*
     * Proteção contra explosão de métricas (high cardinality)
     */
    @Bean
    public MeterFilter denyHighCardinality() {
        return MeterFilter.maximumAllowableTags(
                "lead.created.total",
                "vendor",
                1000,
                MeterFilter.deny()
        );
    }
}