package com.leadflow.backend.service.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.monitoring.MetricsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OpenAiService implements AiService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final MetricsService metricsService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public String generate(String prompt) {

        metricsService.incrementAiCalls();

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

        } catch (Exception e) {
            return "Erro ao gerar resposta da IA.";
        }
    }
}