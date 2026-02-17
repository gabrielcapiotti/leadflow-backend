package com.leadflow.backend.dto.role;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RoleResponse {

    private final Integer id;
    private final String name;

    public RoleResponse(Integer id, String name) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
