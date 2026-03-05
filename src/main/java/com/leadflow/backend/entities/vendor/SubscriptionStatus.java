package com.leadflow.backend.entities.vendor;

public enum SubscriptionStatus {

    TRIAL,
    ATIVA,
    INADIMPLENTE,
    SUSPENSA,
    CANCELADA,
    EXPIRADA;

    public boolean canTransitionTo(SubscriptionStatus target) {

        return switch (this) {

            case TRIAL -> target == ATIVA ||
                          target == CANCELADA ||
                          target == EXPIRADA;

            case ATIVA -> target == INADIMPLENTE ||
                          target == CANCELADA ||
                          target == EXPIRADA ||
                          target == SUSPENSA;

            case INADIMPLENTE -> target == ATIVA ||
                                 target == CANCELADA ||
                                 target == EXPIRADA;

            case SUSPENSA -> target == ATIVA ||
                             target == CANCELADA;

            case CANCELADA -> false;

            case EXPIRADA -> target == ATIVA;
        };
    }
}
