package com.leadflow.backend.specification;

import com.leadflow.backend.entities.audit.SecurityAuditLog;
import com.leadflow.backend.entities.audit.SecurityAction;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SecurityAuditSpecification {

    private SecurityAuditSpecification() {
        // Utility class
    }

    public static Specification<SecurityAuditLog> filter(
            String email,
            String tenant,
            SecurityAction action,
            Boolean success,
            LocalDateTime from,
            LocalDateTime to
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (email != null && !email.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("email")),
                                "%" + email.trim().toLowerCase() + "%"
                        )
                );
            }

            if (tenant != null && !tenant.isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("tenant")),
                                tenant.trim().toLowerCase()
                        )
                );
            }

            if (action != null) {
                predicates.add(
                        cb.equal(root.get("action"), action)
                );
            }

            if (success != null) {
                predicates.add(
                        cb.equal(root.get("success"), success)
                );
            }

            if (from != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("createdAt"), from)
                );
            }

            if (to != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(root.get("createdAt"), to)
                );
            }

            // Ordenação padrão por data desc se não houver sort explícito
            if (query != null && query.getOrderList().isEmpty()) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}