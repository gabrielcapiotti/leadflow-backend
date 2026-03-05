package com.leadflow.backend.repository;

import com.leadflow.backend.entities.billing.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {
}
