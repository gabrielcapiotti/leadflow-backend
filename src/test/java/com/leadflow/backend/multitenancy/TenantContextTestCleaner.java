package com.leadflow.backend.multitenancy;

import com.leadflow.backend.multitenancy.context.TenantContext;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Garante que o TenantContext seja limpo antes e depois de cada teste,
 * evitando vazamento de tenant entre threads de testes.
 */
public final class TenantContextTestCleaner extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        TenantContext.clear();
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        TenantContext.clear();
    }
}