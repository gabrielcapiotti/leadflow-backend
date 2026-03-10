package com.leadflow.backend.service;

import com.leadflow.backend.entities.Plan;
import com.leadflow.backend.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    public Plan getActivePlan() {

        return planRepository.findByActiveTrue()
                .orElseThrow(() -> new RuntimeException("Active plan not configured"));

    }
}
