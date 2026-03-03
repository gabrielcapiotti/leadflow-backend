package com.leadflow.backend.entities.vendor;

import java.util.Set;

public enum LeadStage {

    NOVO,
    CONTATO,
    PROPOSTA,
    FECHADO,
    PERDIDO;

    public boolean canTransitionTo(LeadStage target) {

        return switch (this) {

            case NOVO ->
                    Set.of(CONTATO, PERDIDO).contains(target);

            case CONTATO ->
                    Set.of(PROPOSTA, PERDIDO).contains(target);

            case PROPOSTA ->
                    Set.of(FECHADO, PERDIDO).contains(target);

            case FECHADO ->
                    false;

            case PERDIDO ->
                    false;
        };
    }
}