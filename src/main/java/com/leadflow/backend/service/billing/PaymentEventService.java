package com.leadflow.backend.service.billing;

import com.leadflow.backend.entities.billing.PaymentEvent;
import com.leadflow.backend.repository.PaymentEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventService {

    private final PaymentEventRepository repository;

    public PaymentEventService(PaymentEventRepository repository) {
        this.repository = repository;
    }

    public boolean alreadyProcessed(String eventId) {
        return repository.findByProviderEventId(eventId).isPresent();
    }

    public void register(String eventId, String provider) {
        registerIfFirstProcess(eventId, provider);
    }

    public boolean registerIfFirstProcess(String eventId, String provider) {
        PaymentEvent event = new PaymentEvent();
        event.setProviderEventId(eventId);
        event.setProvider(provider);

        try {
            repository.save(event);
            return true;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }
}