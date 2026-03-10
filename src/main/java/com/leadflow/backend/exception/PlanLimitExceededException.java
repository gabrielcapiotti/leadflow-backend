package com.leadflow.backend.exception;

/**
 * Exception thrown when a tenant exceeds their plan limits.
 * This should be mapped to HTTP 402 (Payment Required) to indicate
 * that the user needs to upgrade their plan.
 */
public class PlanLimitExceededException extends RuntimeException {

    public PlanLimitExceededException(String message) {
        super(message);
    }

}
