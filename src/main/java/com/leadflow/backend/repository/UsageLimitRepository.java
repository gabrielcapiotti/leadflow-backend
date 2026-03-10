package com.leadflow.backend.repository;

import com.leadflow.backend.entities.UsageLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageLimitRepository extends JpaRepository<UsageLimit, Long> {

    /**
     * Finds usage limit by tenantId with pessimistic write lock
     * to prevent race conditions during concurrent updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UsageLimit> findByTenantId(UUID tenantId);

    boolean existsByTenantId(UUID tenantId);

}
