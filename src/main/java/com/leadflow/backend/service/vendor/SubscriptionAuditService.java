package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.SubscriptionHistory;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.repository.SubscriptionHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SubscriptionAuditService {

    private final SubscriptionHistoryRepository repository;

    public SubscriptionAuditService(SubscriptionHistoryRepository repository) {
        this.repository = repository;
    }

    public void record(UUID vendorId,
                       SubscriptionStatus previous,
                       SubscriptionStatus current,
                       String reason,
                       String externalEventId) {

        SubscriptionHistory history = new SubscriptionHistory();
        history.setVendorId(vendorId);
        history.setPreviousStatus(previous);
        history.setNewStatus(current);
        history.setReason(reason);
        history.setExternalEventId(externalEventId);

        repository.save(history);
    }
}
