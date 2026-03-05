package com.leadflow.backend.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.entities.payment.Payment;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.PaymentRepository;
import com.leadflow.backend.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final VendorRepository vendorRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            VendorRepository vendorRepository,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.vendorRepository = vendorRepository;
        this.objectMapper = objectMapper;
    }

    public void processWebhook(String payload) {

        try {

            JsonNode node = objectMapper.readTree(payload);

            String eventId = node.get("id").asText();
            String email = node.get("email").asText();
            String status = node.get("status").asText();

            if (paymentRepository.existsByEventId(eventId)) {
                return;
            }

            Payment payment = new Payment();
            payment.setEventId(eventId);
            payment.setEmail(email);
            payment.setStatus(status);
            payment.setGateway("external");
            payment.setPayload(payload);

            paymentRepository.save(payment);

            if ("paid".equalsIgnoreCase(status)) {
                activateAccount(email);
            }

        } catch (Exception ignored) {}
    }

    private void activateAccount(String email) {

        boolean vendorExists =
                vendorRepository.findByUserEmail(email)
                        .stream()
                        .findFirst()
                        .isPresent();

        if (vendorExists) {
            return;
        }

        Vendor vendor = new Vendor();

        vendor.setUserEmail(email);
        vendor.setNomeVendedor(email);
        vendor.setSlug(generateSlug(email));
        vendor.setStatusAssinatura("ativo");

        vendorRepository.save(vendor);
    }

    private String generateSlug(String email) {
        return email.split("@")[0] + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}