package com.leadflow.backend.service.vendor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.dto.vendor.AlertDTO;
import com.leadflow.backend.entities.vendor.LeadStage;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorLead;
import com.leadflow.backend.entities.vendor.VendorLeadAlert;
import com.leadflow.backend.entities.vendor.VendorLeadConversation;
import com.leadflow.backend.repository.VendorLeadAlertRepository;
import com.leadflow.backend.repository.VendorLeadRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.monitoring.MetricsService;
import com.leadflow.backend.service.notification.SendGridEmailService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ResumoService {

        private static final String RESUMO_FALLBACK =
                        "Resumo estratégico indisponível no momento. Mantenha follow-up com o cliente.";

    private final VendorLeadRepository leadRepository;
        private final VendorLeadAlertRepository alertRepository;
        private final VendorRepository vendorRepository;
    private final ConversationService conversationService;
    private final AiService aiService;
        private final AlertNotificationService notificationService;
                private final SendGridEmailService emailService;
                                private final VendorContext vendorContext;
                                                                private final AuditService auditService;
                                                                private final MetricsService metricsService;

    public ResumoService(VendorLeadRepository leadRepository,
                                                 VendorLeadAlertRepository alertRepository,
                                                 VendorRepository vendorRepository,
                                                 ConversationService conversationService,
                                                 AiService aiService,
                                                 AlertNotificationService notificationService,
                                                 SendGridEmailService emailService,
                                                 VendorContext vendorContext,
                                                 AuditService auditService,
                                                 MetricsService metricsService) {
        this.leadRepository = leadRepository;
                this.alertRepository = alertRepository;
                this.vendorRepository = vendorRepository;
        this.conversationService = conversationService;
        this.aiService = aiService;
                this.notificationService = notificationService;
                this.emailService = emailService;
                this.vendorContext = vendorContext;
                this.auditService = auditService;
                this.metricsService = metricsService;
    }

    public String gerarResumo(UUID leadId) {

        var vendor = vendorContext.getCurrentVendor();

        VendorLead lead = leadRepository.findByIdAndVendorId(leadId, vendor.getId())
                .orElseThrow(() -> new RuntimeException("Lead não encontrado ou acesso negado"));

        List<VendorLeadConversation> history =
                conversationService.getConversation(leadId);

        StringBuilder contexto = new StringBuilder();

        for (VendorLeadConversation msg : history) {
            contexto.append(msg.getRole())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }

        String prompt = """
                                Você é um analista comercial.

                                Analise a conversa abaixo e retorne EXCLUSIVAMENTE um JSON no formato:

                                {
                                  "resumo": "...",
                                  "nivelInteresse": 0-100,
                                  "probabilidadeFechamento": 0-100,
                                                                                                                                        "recomendacao": "...",
                                                                                                                                        "stageSugerido": "NOVO|CONTATO|PROPOSTA|FECHADO|PERDIDO"
                                }

                                Conversa:
                                """ + contexto;

                String resposta = aiService.generate(prompt);

                if (resposta == null || resposta.isBlank()) {
                        lead.setResumoEstrategico(RESUMO_FALLBACK);
                        leadRepository.save(lead);
                        return RESUMO_FALLBACK;
                }

                try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(resposta);

                        String resumo = node.hasNonNull("resumo")
                                        ? node.get("resumo").asText()
                                        : RESUMO_FALLBACK;

                        int scoreAtual = lead.getScore() != null ? lead.getScore() : 0;
                        int nivelInteresse = extrairScore(node, "nivelInteresse", scoreAtual);
                        int probFechamento = extrairScore(node, "probabilidadeFechamento", scoreAtual);
                        LeadStage novoStage = extrairStageSugerido(node);

                        aplicarTransicaoSugerida(lead, novoStage);

                        lead.setResumoEstrategico(resumo);

                        int scoreFinal = calcularScoreSemantico(
                                        lead,
                                        nivelInteresse,
                                        probFechamento
                        );

                        lead.setScore(scoreFinal);
                        leadRepository.save(lead);

                        auditService.log(
                                        "RESUMO_GERADO",
                                        "VendorLead",
                                        lead.getId(),
                                        "Resumo estratégico atualizado"
                        );

                        avaliarAlerta(lead, nivelInteresse, probFechamento);

                        return resumo;
                } catch (Exception e) {
                        lead.setResumoEstrategico(RESUMO_FALLBACK);
                        leadRepository.save(lead);
                        return RESUMO_FALLBACK;
                }
    }

        private void aplicarTransicaoSugerida(VendorLead lead, LeadStage novoStage) {
                LeadStage atual = lead.getStage();

                if (atual != null &&
                                novoStage != null &&
                                atual.canTransitionTo(novoStage)) {

                        lead.setStage(novoStage);
                }
        }

        private int calcularScoreSemantico(VendorLead lead,
                                                                           int nivelInteresse,
                                                                           int probFechamento) {

                int heuristico = 0;

                if (lead.getUrgencia() != null) {
                        heuristico = switch (lead.getUrgencia()) {
                                case "quero_fechar" -> 100;
                                case "analisando" -> 60;
                                case "pesquisando" -> 30;
                                default -> 10;
                        };
                }

                int stageBonus = switch (lead.getStage()) {
                        case PROPOSTA -> 20;
                        case CONTATO -> 10;
                        default -> 0;
                };

                int score = (int) (
                                heuristico * 0.3 +
                                nivelInteresse * 0.3 +
                                probFechamento * 0.4 +
                                stageBonus
                );

                return clamp(score);
        }

        private int extrairScore(JsonNode node, String campo, int fallback) {
                if (node.hasNonNull(campo)) {
                        return clamp(node.get(campo).asInt());
                }
                return clamp(fallback);
        }

        private void avaliarAlerta(VendorLead lead,
                                                           int nivelInteresse,
                                                           int probFechamento) {

                boolean hot = lead.getScore() >= 85
                                || probFechamento >= 80
                                || (lead.getStage() == LeadStage.PROPOSTA
                                && nivelInteresse >= 75);

                if (!hot) {
                        return;
                }

                boolean jaExiste = !alertRepository
                                                                                .findByVendorLeadIdAndResolvidoFalseOrderByCreatedAtDesc(lead.getId())
                                .isEmpty();

                if (jaExiste) {
                        return;
                }

                VendorLeadAlert alert = new VendorLeadAlert();
                alert.setVendorLeadId(lead.getId());
                alert.setTipo("HOT_LEAD");
                alert.setMensagem("Lead com alta probabilidade de fechamento.");

                VendorLeadAlert savedAlert = alertRepository.save(alert);

                metricsService.incrementHotLead();

                auditService.log(
                                "ALERTA_CRIADO",
                                "VendorLeadAlert",
                                lead.getId(),
                                "Alerta HOT_LEAD criado"
                );

                String destinationEmail = resolverEmailDestino(lead);
                if (destinationEmail == null || destinationEmail.isBlank()) {
                        return;
                }

                notificationService.notifyUser(
                                destinationEmail,
                                new AlertDTO(
                                                savedAlert.getTipo(),
                                                savedAlert.getMensagem(),
                                                lead.getId().toString()
                                )
                );

                int score = lead.getScore() != null ? lead.getScore() : 0;
                                                                emailService.sendEmail(
                                                                                                                                destinationEmail,
                                                                                                                                "🔥 Lead Quente Detectado",
                                                                                                                                """
                                                                                                                                <html>
                                                                                                                                <body style="font-family: Arial; background:#f4f4f4; padding:20px;">
                                                                                                                                        <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px;">
                                                                                                                                                <h2 style="color:#8B0000;">🔥 Lead Quente Detectado</h2>
                                                                                                                                                <p>Um lead com alta probabilidade de fechamento foi detectado.</p>
                                                                                                                                                <p><strong>Lead ID:</strong> %s</p>
                                                                                                                                                <p><strong>Score:</strong> %d</p>
                                                                                                                                                <p>Acesse o sistema para agir imediatamente.</p>
                                                                                                                                                <p style="margin-top:30px; font-size:12px; color:#888;">Leadflow AI</p>
                                                                                                                                        </div>
                                                                                                                                </body>
                                                                                                                                </html>
                                                                                                                                """.formatted(lead.getId(), score)
                                                                );
        }

        private String resolverEmailDestino(VendorLead lead) {
                if (lead.getOwnerEmail() != null && !lead.getOwnerEmail().isBlank()) {
                        return lead.getOwnerEmail();
                }

                return vendorRepository.findById(lead.getVendorId())
                                .map(Vendor::getUserEmail)
                                .orElse(null);
        }

        private LeadStage extrairStageSugerido(JsonNode node) {
                if (!node.hasNonNull("stageSugerido")) {
                        return null;
                }

                try {
                        String stageSugerido = node.get("stageSugerido").asText();
                        if (stageSugerido == null || stageSugerido.isBlank()) {
                                return null;
                        }

                        return LeadStage.valueOf(stageSugerido.trim().toUpperCase());
                } catch (Exception e) {
                        return null;
                }
        }

        private int clamp(int valor) {
                return Math.max(0, Math.min(valor, 100));
        }
}
