package com.leadflow.backend.multitenancy.hibernate;

import com.leadflow.backend.multitenancy.context.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {

        String tenant = TenantContext.getTenant();

        if (tenant == null) {
            return "public";
        }

        return tenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}