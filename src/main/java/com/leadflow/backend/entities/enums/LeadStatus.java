package com.leadflow.backend.entities.enums;

public enum LeadStatus {

    NEW,
    CONTACTED,
    QUALIFIED,
    CLOSED;

    public boolean canTransitionTo(LeadStatus next) {
        if (next == null) return false;

        // Permitir mesma transição (idempotência)
        if (this == next) return true;

        return switch (this) {
            case NEW -> next == CONTACTED;
            case CONTACTED -> next == QUALIFIED;
            case QUALIFIED -> next == CLOSED;
            case CLOSED -> false; // Terminal state
        };
    }
}
