package com.leadflow.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(

        @JsonProperty("accessToken")
        String accessToken,

        @JsonProperty("refreshToken")
        String refreshToken

) {

    public AuthResponse {

        if (accessToken == null) {
            throw new IllegalArgumentException("Access token cannot be null");
        }

        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token cannot be null");
        }

        accessToken = accessToken.trim();
        refreshToken = refreshToken.trim();

        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be blank");
        }

        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be blank");
        }
    }
}