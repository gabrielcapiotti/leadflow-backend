package com.leadflow.backend.repository;

import com.leadflow.backend.entities.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByEventId(String eventId);

    Optional<Payment> findByEventId(String eventId);
}