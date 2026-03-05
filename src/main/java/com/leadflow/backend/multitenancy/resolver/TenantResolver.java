package com.leadflow.backend.multitenancy.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver {

    public String resolveTenant(HttpServletRequest request) {

        String tenant = request.getHeader("X-Tenant-ID");

        if (tenant == null || tenant.isBlank()) {
            throw new RuntimeException("Tenant não informado");
        }

        return tenant;
    }
}