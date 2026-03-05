package com.leadflow.backend.dto.vendor;

import java.time.Instant;

public class UsageResponse {

    private final ResourceUsage active_leads;
    private final ResourceUsage ai_executions;
    private final Instant period_end;

    public static class ResourceUsage {
        private final int used;
        private final int limit;
        private final int percentage;

        public ResourceUsage(int used, int limit) {
            this.used = used;
            this.limit = limit;
            this.percentage = limit > 0
                    ? (int) ((used / (double) limit) * 100)
                    : 0;
        }

        public int getUsed() {
            return used;
        }

        public int getLimit() {
            return limit;
        }

        public int getPercentage() {
            return percentage;
        }
    }

    public UsageResponse(ResourceUsage activeLeads,
                         ResourceUsage aiExecutions,
                         Instant periodEnd) {
        this.active_leads = activeLeads;
        this.ai_executions = aiExecutions;
        this.period_end = periodEnd;
    }

    public ResourceUsage getActive_leads() {
        return active_leads;
    }

    public ResourceUsage getAi_executions() {
        return ai_executions;
    }

    public Instant getPeriod_end() {
        return period_end;
    }
}
