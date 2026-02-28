package com.leadflow.backend.controller.auth;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshRequest(

        @JsonProperty("refreshToken")
        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {

    public RefreshRequest {

        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token cannot be null");
        }

        refreshToken = refreshToken.trim();

        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be blank");
        }
    }
}