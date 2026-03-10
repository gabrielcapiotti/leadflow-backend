package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.Plan;
import com.leadflow.backend.entities.UsageLimit;
import com.leadflow.backend.exception.PlanLimitExceededException;
import com.leadflow.backend.repository.UsageLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Professional SaaS usage control service.
 * 
 * Responsibilities:
 * - Initialize usage limits when a plan is activated
 * - Atomically validate and consume resources (leads, users, AI executions)
 * - Query current usage state
 * 
 * Thread-safety: Uses pessimistic locking to prevent race conditions.
 * Transactional consistency: Validation + increment happens in a single atomic transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsageService {

    private final UsageLimitRepository usageRepository;

    /* =========================================================
       INTERNAL LOADER (with pessimistic lock)
       ========================================================= */

    /**
     * Loads usage with pessimistic write lock to prevent concurrent modifications.
     * This is called inside @Transactional methods to ensure atomic operations.
     */
    private UsageLimit loadUsage(UUID tenantId) {
        return usageRepository.findByTenantId(tenantId)
                .orElseThrow(() ->
                        new RuntimeException("Usage configuration not found for tenant " + tenantId)
                );
    }

    /* =========================================================
       INITIALIZATION
       ========================================================= */

    /**
     * Inicializa os limites de uso para um tenant quando o pagamento é confirmado.
     * Se já existir registro para o tenant, não cria duplicata.
     *
     * @param tenantId ID do tenant (vendor)
     * @param plan Plano contratado
     */
    @Transactional
    public UsageLimit initializeUsage(UUID tenantId, Plan plan) {
        
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }

        // Verifica se já existe registro para evitar duplicatas
        if (usageRepository.existsByTenantId(tenantId)) {
            UsageLimit existingUsage = getUsage(tenantId);
            log.info("Usage already initialized for tenant {}", tenantId);
            return existingUsage;
        }

        UsageLimit usage = new UsageLimit();

        usage.setTenantId(tenantId);
        usage.setPlan(plan);

        usage.setLeadsUsed(0);
        usage.setUsersUsed(0);
        usage.setAiExecutionsUsed(0);

        UsageLimit savedUsage = usageRepository.save(usage);
        log.info("Usage initialized for tenant {}", tenantId);
        return savedUsage;
    }

    /* =========================================================
       ATOMIC CONSUMPTION (Professional SaaS Pattern)
       ========================================================= */

    /**
     * Atomically validates and consumes a lead slot.
     * This method is thread-safe and prevents race conditions through pessimistic locking.
     * 
     * @param tenantId ID of the tenant
     * @throws PlanLimitExceededException if lead limit is reached
     */
    @Transactional
    public void consumeLead(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);

        if (usage.getLeadsUsed() >= usage.getPlan().getMaxLeads()) {
            throw new PlanLimitExceededException("Lead limit reached");
        }

        usage.setLeadsUsed(usage.getLeadsUsed() + 1);
        usageRepository.save(usage);

        log.debug("Lead consumed for tenant {} ({}/{})", 
                tenantId, usage.getLeadsUsed(), usage.getPlan().getMaxLeads());
    }

    /**
     * Atomically validates and consumes a user slot.
     * This method is thread-safe and prevents race conditions through pessimistic locking.
     * 
     * @param tenantId ID of the tenant
     * @throws PlanLimitExceededException if user limit is reached
     */
    @Transactional
    public void consumeUser(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);

        if (usage.getUsersUsed() >= usage.getPlan().getMaxUsers()) {
            throw new PlanLimitExceededException("User limit reached");
        }

        usage.setUsersUsed(usage.getUsersUsed() + 1);
        usageRepository.save(usage);

        log.debug("User consumed for tenant {} ({}/{})", 
                tenantId, usage.getUsersUsed(), usage.getPlan().getMaxUsers());
    }

    /**
     * Atomically validates and consumes an AI execution slot.
     * This method is thread-safe and prevents race conditions through pessimistic locking.
     * 
     * @param tenantId ID of the tenant
     * @throws PlanLimitExceededException if AI execution limit is reached
     */
    @Transactional
    public void consumeAiExecution(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);

        if (usage.getAiExecutionsUsed() >= usage.getPlan().getMaxAiExecutions()) {
            throw new PlanLimitExceededException("AI execution limit reached");
        }

        usage.setAiExecutionsUsed(usage.getAiExecutionsUsed() + 1);
        usageRepository.save(usage);

        log.debug("AI execution consumed for tenant {} ({}/{})", 
                tenantId, usage.getAiExecutionsUsed(), usage.getPlan().getMaxAiExecutions());
    }

    /* =========================================================
       LEGACY VALIDATION (backward compatibility, read-only)
       ========================================================= */

    @Transactional(readOnly = true)
    public void validateLeadLimit(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);

        if (usage.getLeadsUsed() >= usage.getPlan().getMaxLeads()) {
            throw new PlanLimitExceededException("Lead limit reached");
        }
    }

    @Transactional(readOnly = true)
    public void validateUserLimit(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);

        if (usage.getUsersUsed() >= usage.getPlan().getMaxUsers()) {
            throw new PlanLimitExceededException("User limit reached");
        }
    }

    @Transactional(readOnly = true)
    public void validateAiUsage(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);

        if (usage.getAiExecutionsUsed() >= usage.getPlan().getMaxAiExecutions()) {
            throw new PlanLimitExceededException("AI execution limit reached");
        }
    }

    @Transactional(readOnly = true)
    public void validateAiExecutionLimit(UUID tenantId) {
        validateAiUsage(tenantId);
    }

    /* =========================================================
       LEGACY INCREMENT (backward compatibility, separate tx)
       ========================================================= */

    @Transactional
    public void incrementLeadsUsed(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);
        usage.setLeadsUsed(usage.getLeadsUsed() + 1);
        usageRepository.save(usage);
    }

    @Transactional
    public void incrementUsersUsed(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);
        usage.setUsersUsed(usage.getUsersUsed() + 1);
        usageRepository.save(usage);
    }

    @Transactional
    public void incrementAiExecutionsUsed(UUID tenantId) {
        UsageLimit usage = loadUsage(tenantId);
        usage.setAiExecutionsUsed(usage.getAiExecutionsUsed() + 1);
        usageRepository.save(usage);
    }

    @Transactional
    public void incrementLeads(UUID tenantId) {
        incrementLeadsUsed(tenantId);
    }

    @Transactional
    public void incrementUsers(UUID tenantId) {
        incrementUsersUsed(tenantId);
    }

    @Transactional
    public void incrementAiExecutions(UUID tenantId) {
        incrementAiExecutionsUsed(tenantId);
    }

    /* =========================================================
       QUERY
       ========================================================= */

    @Transactional(readOnly = true)
    public UsageLimit getUsage(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }

        return usageRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Usage not found for tenant"));
    }

}
