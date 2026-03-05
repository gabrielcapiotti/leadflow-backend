package com.leadflow.backend.specification;

import com.leadflow.backend.entities.vendor.VendorAuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class VendorAuditSpecification {

    private VendorAuditSpecification() {
    }

    public static Specification<VendorAuditLog> filter(
            UUID vendorId,
            String acao,
            String entityType,
            Instant from,
            Instant to
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (vendorId != null) {
                predicates.add(cb.equal(root.get("vendorId"), vendorId));
            }

            if (acao != null && !acao.isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("acao")),
                                acao.trim().toLowerCase()
                        )
                );
            }

            if (entityType != null && !entityType.isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("entityType")),
                                entityType.trim().toLowerCase()
                        )
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

            if (query != null && query.getOrderList().isEmpty()) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}