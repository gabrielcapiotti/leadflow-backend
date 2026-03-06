package com.leadflow.backend.multitenancy.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Component
public class TenantResolver {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    private static final Pattern TENANT_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]+$");

    public String resolveTenant(HttpServletRequest request) {

        String tenant = request.getHeader(TENANT_HEADER);

        if (tenant == null || tenant.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Header 'X-Tenant-ID' é obrigatório"
            );
        }

        if (!TENANT_PATTERN.matcher(tenant).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identificador de tenant inválido"
            );
        }

        return tenant;
    }
}