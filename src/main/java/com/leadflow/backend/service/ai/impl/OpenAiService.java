package com.leadflow.backend.service.ai.impl;

import com.leadflow.backend.audit.Audit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.quota.CheckQuota;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.monitoring.MetricsService;
import com.leadflow.backend.service.monitoring.SystemAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

@Service
public class OpenAiService implements AiService {

        private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    private final MetricsService metricsService;
    private final SystemAlertService systemAlertService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiService(MetricsService metricsService,
                         SystemAlertService systemAlertService) {
        this.metricsService = metricsService;
        this.systemAlertService = systemAlertService;
    }

    @Override
    @Audit(action = "AI_EXECUTION", entity = "AI")
    @CheckQuota(type = "IA_EXECUTION")
    @Retryable(
            retryFor = {
                    HttpClientErrorException.TooManyRequests.class,
                    HttpServerErrorException.class,
                    ResourceAccessException.class
            },
            maxAttemptsExpression = "#{${openai.retry.max-attempts:3}}",
            backoff = @Backoff(
                    delayExpression = "#{${openai.retry.initial-backoff-ms:1500}}",
                    multiplierExpression = "#{${openai.retry.multiplier:2.0}}"
            )
    )
    public String generate(String prompt) {

                metricsService.aiExecution();
                log.info("ai_execution prompt_size={}", prompt != null ? prompt.length() : 0);

        try {

            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String systemPrompt = """
                Você é um assistente comercial.
                Extraia os seguintes campos da conversa:

                - nomeCompleto
                - whatsapp
                - tipoConsorcio
                - valorCredito
                - urgencia (quero_fechar | analisando | pesquisando)

                Responda em JSON válido.
            """;

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", new Object[]{
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", prompt)
                    }
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            return jsonNode
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

                } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException | ResourceAccessException e) {
            throw e;
        } catch (Exception e) {
            systemAlertService.sendCriticalAlert(
                    "Falha no serviço de IA: " + e.getMessage()
            );
            return "Erro ao gerar resposta da IA.";
        }
    }

    @Recover
    public String recoverOpenAi(Throwable e, String prompt) {
        systemAlertService.sendCriticalAlert(
                "Falha no serviço de IA após retentativas: " + e.getMessage()
        );
        return "Erro ao gerar resposta da IA.";
    }
}