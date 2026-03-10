package com.leadflow.backend.repository;

import com.leadflow.backend.entities.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByNameIgnoreCase(String name);

    Optional<Plan> findByActiveTrue();
}
