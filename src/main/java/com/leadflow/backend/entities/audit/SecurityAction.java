package com.leadflow.backend.entities.audit;

public enum SecurityAction {

    LOGIN_SUCCESS,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_SUCCESS,
    USER_REGISTERED
}