package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.EmailEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EmailEventRepository extends JpaRepository<EmailEvent, UUID> {

	List<EmailEvent> findTop50ByStatusAndNextRetryAtLessThanEqualOrderByOccurredAtAsc(
			String status,
			Instant nextRetryAt
	);
}
