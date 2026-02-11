package com.leadflow.backend.dto.role;

public class RoleResponse {

    private Integer id;
    private String name;

    public RoleResponse(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
