package com.leadflow.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class UserResponse {

    private final Long id;
    private final String name;
    private final String email;

    @JsonProperty("role")
    private final String role;

    public UserResponse(
            Long id,
            String name,
            String email,
            String role
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
