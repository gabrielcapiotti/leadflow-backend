package com.leadflow.backend.service.vendor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.audit.Audit;
import com.leadflow.backend.dto.vendor.CreateLeadRequest;
import com.leadflow.backend.dto.vendor.StageConversionResponse;
import com.leadflow.backend.dto.vendor.StageTimeMetricsResponse;
import com.leadflow.backend.dto.vendor.VendorLeadMetricsResponse;
import com.leadflow.backend.entities.vendor.LeadStage;
import com.leadflow.backend.entities.vendor.VendorLead;
import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.entities.vendor.VendorLeadStageHistory;
import com.leadflow.backend.repository.VendorLeadRepository;
import com.leadflow.backend.repository.VendorLeadStageHistoryRepository;
import com.leadflow.backend.repository.vendor.VendorLeadConversationRepository;
import com.leadflow.backend.quota.CheckQuota;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.monitoring.MetricsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class VendorLeadService {

    private static final Logger log = LoggerFactory.getLogger(VendorLeadService.class);

    private static final Pattern WHATSAPP_PATTERN =
            Pattern.compile("^[0-9+()\\-\\s]{8,20}$");

    private static final Pattern VALOR_CREDITO_PATTERN =
            Pattern.compile("^[0-9.,\\s]{1,30}$");

    private final VendorLeadRepository repository;
    private final VendorLeadConversationRepository conversationRepository;
    private final VendorLeadStageHistoryRepository historyRepository;
    private final VendorContext vendorContext;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    public VendorLeadService(
            VendorLeadRepository repository,
            VendorLeadConversationRepository conversationRepository,
            VendorLeadStageHistoryRepository historyRepository,
            VendorContext vendorContext,
            MetricsService metricsService,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.conversationRepository = conversationRepository;
        this.historyRepository = historyRepository;
        this.vendorContext = vendorContext;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
    }

    @Audit(action = "CREATE_LEAD_FROM_AI", entity = "VendorLead")
    @CheckQuota(type = "LEAD_CREATION")
    public VendorLead createFromAi(UUID vendorId, String json) {

        try {

            JsonNode node = objectMapper.readTree(json);

            if (node.hasNonNull("nomeCompleto") &&
                node.hasNonNull("whatsapp")) {

                String nomeCompleto =
                        sanitizeNomeCompleto(node.get("nomeCompleto").asText());

                String whatsapp =
                        sanitizeWhatsapp(node.get("whatsapp").asText());

                String valorCredito =
                        sanitizeValorCredito(node.path("valorCredito").asText(null));

                if (nomeCompleto == null || whatsapp == null) {
                    return null;
                }

                var existingLead =
                        repository.findFirstByVendorIdAndWhatsappOrderByCreatedDateDesc(
                                vendorId,
                                whatsapp
                        );

                boolean isNewLead = existingLead.isEmpty();

                VendorLead lead = existingLead.orElseGet(VendorLead::new);

                if (lead.getVendorId() == null) {
                    lead.setVendorId(vendorId);
                }

                lead.setNomeCompleto(nomeCompleto);
                lead.setWhatsapp(whatsapp);
                lead.setTipoConsorcio(node.path("tipoConsorcio").asText(null));
                lead.setValorCredito(valorCredito);
                lead.setUrgencia(node.path("urgencia").asText(null));

                lead.setScore(calculateScore(lead));

                VendorLead savedLead = repository.save(lead);

                if (isNewLead) {

                    metricsService.incrementLeadCreated();
                    metricsService.incrementLeadCreated(vendorId.toString());

                    log.info(
                            "lead_created vendor={} lead={}",
                            vendorId,
                            savedLead.getId()
                    );
                }

                return savedLead;
            }

        } catch (Exception e) {

            log.error(
                    "error_creating_lead_from_ai vendor={} json={}",
                    vendorId,
                    json,
                    e
            );
        }

        return null;
    }

    @Audit(action = "CREATE_LEAD", entity = "VendorLead")
    @CheckQuota(type = "LEAD_CREATION")
    public VendorLead create(CreateLeadRequest request) {

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        String nomeCompleto = sanitizeNomeCompleto(request.getNomeCompleto());
        String whatsapp = sanitizeWhatsapp(request.getWhatsapp());
        String valorCredito = sanitizeValorCredito(request.getValorCredito());

        if (nomeCompleto == null || whatsapp == null) {
            throw new IllegalArgumentException("Dados do lead inválidos");
        }

        VendorLead lead = new VendorLead();

        lead.setVendorId(vendorId);
        lead.setNomeCompleto(nomeCompleto);
        lead.setWhatsapp(whatsapp);
        lead.setTipoConsorcio(request.getTipoConsorcio());
        lead.setValorCredito(valorCredito);
        lead.setUrgencia(request.getUrgencia());

        lead.setScore(calculateScore(lead));

        VendorLead savedLead = repository.save(lead);

        metricsService.incrementLeadCreated();
        metricsService.incrementLeadCreated(vendorId.toString());

        log.info(
                "lead_created vendor={} lead={}",
                vendorId,
                savedLead.getId()
        );

        return savedLead;
    }

    private String sanitizeNomeCompleto(String value) {

        if (value == null) {
            return null;
        }

        String sanitized = value.trim().replaceAll("\\s+", " ");

        if (sanitized.length() < 2 || sanitized.length() > 100) {
            return null;
        }

        return sanitized;
    }

    private String sanitizeWhatsapp(String value) {

        if (value == null) {
            return null;
        }

        String sanitized = value.trim();

        if (!WHATSAPP_PATTERN.matcher(sanitized).matches()) {
            return null;
        }

        return sanitized;
    }

    private String sanitizeValorCredito(String value) {

        if (value == null) {
            return null;
        }

        String sanitized = value.trim();

        if (sanitized.isBlank()) {
            return null;
        }

        if (!VALOR_CREDITO_PATTERN.matcher(sanitized).matches()) {
            return null;
        }

        return sanitized;
    }

    public void saveConversation(UUID leadId, String userMessage, String aiMessage) {

        if (leadId == null || userMessage == null || aiMessage == null) {
            return;
        }

        if (userMessage.isBlank() || aiMessage.isBlank()) {
            return;
        }

        saveConversationMessage(leadId, "USER", userMessage);
        saveConversationMessage(leadId, "AI", aiMessage);
    }

    public List<VendorLeadConversation> getConversation(UUID leadId) {
        return conversationRepository
                .findByVendorLeadIdOrderByCreatedAtAsc(leadId);
    }

    @Audit(action = "UPDATE_STAGE", entity = "VendorLead")
    public VendorLead updateStage(UUID leadId, LeadStage newStage) {

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        VendorLead lead =
                repository.findByIdAndVendorId(leadId, vendorId)
                        .orElseThrow(() ->
                                new RuntimeException("Lead não encontrado ou acesso negado"));

        LeadStage currentStage = lead.getStage();

        if (currentStage == newStage) {
            return lead;
        }

        if (!currentStage.canTransitionTo(newStage)) {

            throw new IllegalStateException(
                    "Transição inválida: " +
                            currentStage +
                            " → " +
                            newStage
            );
        }

        lead.setStage(newStage);
        lead.setScore(calculateScore(lead));

        repository.save(lead);

        log.info(
                "lead_stage_changed vendorId={} leadId={} from={} to={}",
                vendorId,
                leadId,
                currentStage,
                newStage
        );

        VendorLeadStageHistory history = new VendorLeadStageHistory();

        history.setVendorLeadId(leadId);
        history.setPreviousStage(currentStage.name());
        history.setNewStage(newStage.name());

        historyRepository.save(history);

        return lead;
    }

    @Audit(action = "ASSIGN_OWNER", entity = "VendorLead")
    public VendorLead assignOwner(UUID leadId) {

        var vendor = vendorContext.getCurrentVendor();

        VendorLead lead =
                repository.findByIdAndVendorId(leadId, vendor.getId())
                        .orElseThrow(() ->
                                new RuntimeException("Lead não encontrado ou acesso negado"));

        lead.setOwnerEmail(vendor.getUserEmail());

        VendorLead savedLead = repository.save(lead);

        log.info(
                "lead_owner_assigned vendorId={} leadId={} ownerEmail={}",
                vendor.getId(),
                leadId,
                vendor.getUserEmail()
        );

        return savedLead;
    }

    public List<VendorLead> getRankingByOwner(String ownerEmail) {
        return repository.findByOwnerEmailOrderByScoreDesc(ownerEmail);
    }

    public VendorLeadMetricsResponse getMetricsForCurrentVendor() {

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        var results = repository.countByStage(vendorId);

        Map<String, Long> map = new HashMap<>();

        for (Object[] row : results) {

            String stage = (String) row[0];
            Long count = (Long) row[1];

            map.put(stage, count);
        }

        return new VendorLeadMetricsResponse(map);
    }

    public List<VendorLead> getRankingForCurrentVendor() {

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        return repository.findByVendorIdOrderByScoreDesc(vendorId);
    }

    public Page<VendorLead> listForCurrentVendor(Pageable pageable) {

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        return repository.findByVendorId(vendorId, pageable);
    }

    public VendorLead getLeadForCurrentVendor(UUID leadId) {

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        return repository
                .findByIdAndVendorId(leadId, vendorId)
                .orElseThrow(() ->
                        new RuntimeException("Lead não encontrado ou acesso negado"));
    }

    private int calculateScore(VendorLead lead) {

        int base =
                switch (lead.getUrgencia() == null ? "" : lead.getUrgencia()) {

                    case "quero_fechar" -> 100;
                    case "analisando" -> 60;
                    case "pesquisando" -> 30;

                    default -> 10;
                };

        int bonus = 0;

        if (lead.getStage() != null) {

            bonus =
                    switch (lead.getStage()) {

                        case PROPOSTA -> 20;
                        case CONTATO -> 10;

                        default -> 0;
                    };
        }

        return base + bonus;
    }

    public StageTimeMetricsResponse calculateAverageStageTimeForCurrentVendor() {

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        var leads = repository.findByVendorId(vendorId);

        Map<String, List<Long>> stageDurations = new HashMap<>();

        for (var lead : leads) {

            var history =
                    historyRepository
                            .findByVendorLeadIdOrderByChangedAtDesc(lead.getId());

            Instant previous = lead.getCreatedDate();

            for (int i = history.size() - 1; i >= 0; i--) {

                var record = history.get(i);

                long duration =
                        java.time.Duration
                                .between(previous, record.getChangedAt())
                                .toHours();

                stageDurations
                        .computeIfAbsent(
                                record.getPreviousStage(),
                                k -> new ArrayList<>()
                        )
                        .add(duration);

                previous = record.getChangedAt();
            }
        }

        Map<String, Double> averages = new HashMap<>();

        for (var entry : stageDurations.entrySet()) {

            double avg =
                    entry.getValue()
                            .stream()
                            .mapToLong(Long::longValue)
                            .average()
                            .orElse(0);

            averages.put(entry.getKey(), avg);
        }

        return new StageTimeMetricsResponse(averages);
    }

    public StageConversionResponse calculateConversionRatesForCurrentVendor() {

        UUID vendorId = vendorContext.getCurrentVendor().getId();

        var results =
                historyRepository.countTransitionsByVendor(vendorId);

        Map<String, Long> stageTotals = new HashMap<>();
        Map<String, Long> transitions = new HashMap<>();

        for (Object[] row : results) {

            String from = (String) row[0];
            String to = (String) row[1];
            Long count = (Long) row[2];

            stageTotals.put(
                    from,
                    stageTotals.getOrDefault(from, 0L) + count
            );

            transitions.put(from + "→" + to, count);
        }

        Map<String, Double> conversion = new HashMap<>();

        for (var entry : transitions.entrySet()) {

            String from = entry.getKey().split("→")[0];

            long totalFrom = stageTotals.getOrDefault(from, 1L);

            double rate =
                    (double) entry.getValue() / totalFrom * 100.0;

            conversion.put(entry.getKey(), rate);
        }

        return new StageConversionResponse(conversion);
    }

    private void saveConversationMessage(UUID leadId, String role, String content) {

        VendorLeadConversation conversation = new VendorLeadConversation();

        conversation.setVendorLeadId(leadId);
        conversation.setRole(role);
        conversation.setContent(content.trim());

        conversationRepository.save(conversation);
    }
}