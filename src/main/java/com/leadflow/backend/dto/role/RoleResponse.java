package com.leadflow.backend.dto.role;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RoleResponse {

    private final UUID id;
    private final String name;

    public RoleResponse(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
