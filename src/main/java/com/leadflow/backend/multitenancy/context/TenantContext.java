package com.leadflow.backend.multitenancy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Pattern;

public final class TenantContext {

    private static final Logger logger = LoggerFactory.getLogger(TenantContext.class);

    private static final String DEFAULT_SCHEMA = "public";

    // Regex para garantir que o nome do tenant seja seguro
    private static final Pattern VALID_SCHEMA = Pattern.compile("^[a-z0-9_]+$");

    private static final ThreadLocal<String> CURRENT_TENANT = ThreadLocal.withInitial(() -> DEFAULT_SCHEMA);

    private TenantContext() {
        // Utility class
    }

    /* ======================================================
       SET
       ====================================================== */

    /**
     * Define o tenant no contexto atual da thread.
     * Caso o tenant seja inválido ou em branco, o tenant padrão será utilizado.
     * 
     * @param tenant O identificador do tenant
     */
    public static void setTenant(String tenant) {

        // Verifica se o tenant é nulo ou em branco e define o valor default
        if (Objects.isNull(tenant) || tenant.isBlank()) {
            logger.debug("Null/blank tenant received. Using default schema.");
            CURRENT_TENANT.set(DEFAULT_SCHEMA);
            return;
        }

        String normalized = tenant.trim().toLowerCase();  // Remove espaços em excesso e padroniza para minúsculas

        // Valida se o tenant é compatível com o padrão permitido
        if (!VALID_SCHEMA.matcher(normalized).matches()) {
            logger.error("Invalid tenant identifier: {}", tenant);  // Log mais detalhado no caso de erro
            throw new IllegalArgumentException("Invalid tenant identifier: " + tenant);
        }

        CURRENT_TENANT.set(normalized);
        logger.debug("Tenant set to: {}", normalized);  // Log para rastrear a alteração do tenant
    }

    /* ======================================================
       GET
       ====================================================== */

    /**
     * Obtém o tenant do contexto atual.
     * 
     * @return O identificador do tenant
     */
    public static String getTenant() {
        return CURRENT_TENANT.get();
    }

    /* ======================================================
       CLEAR
       ====================================================== */

    /**
     * Limpa o tenant do contexto atual.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
        logger.debug("Tenant context cleared.");
    }
}