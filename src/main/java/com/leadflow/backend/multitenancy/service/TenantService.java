package com.leadflow.backend.multitenancy.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final JdbcTemplate jdbcTemplate;

    public TenantService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createTenantSchema(String schemaName) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    }
}