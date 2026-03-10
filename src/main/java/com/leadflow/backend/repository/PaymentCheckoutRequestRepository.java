package com.leadflow.backend.repository;

import com.leadflow.backend.entities.billing.PaymentCheckoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentCheckoutRequestRepository extends JpaRepository<PaymentCheckoutRequest, UUID> {

    Optional<PaymentCheckoutRequest> findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(String email, String status);

    Optional<PaymentCheckoutRequest> findTopByReferenceIdOrderByCreatedAtDesc(String referenceId);
}
