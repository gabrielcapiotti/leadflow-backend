package com.leadflow.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(

        @JsonProperty("token")
        String token

) {

    public AuthResponse {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }

        token = token.trim();

        if (token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be blank");
        }
    }
}