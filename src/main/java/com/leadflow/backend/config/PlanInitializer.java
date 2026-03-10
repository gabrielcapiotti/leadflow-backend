package com.leadflow.backend.config;

import com.leadflow.backend.entities.Plan;
import com.leadflow.backend.repository.PlanRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanInitializer {

    private static final Logger log = LoggerFactory.getLogger(PlanInitializer.class);

    private final PlanRepository planRepository;

    @PostConstruct
    public void initializePlan() {

        if (planRepository.count() == 0) {

            Plan plan = new Plan();

            plan.setName("Leadflow Standard");
            plan.setMaxLeads(500);
            plan.setMaxUsers(10);
            plan.setMaxAiExecutions(1000);
            plan.setActive(true);

            planRepository.save(plan);

            log.info("Plano padrão 'Leadflow Standard' criado automaticamente no startup");
        } else {
            log.info("Plano já existe no banco de dados");
        }
    }
}
