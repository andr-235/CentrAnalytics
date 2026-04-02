package com.ca.centranalytics.integration.channel.telegram.controller;

import com.ca.centranalytics.integration.channel.telegram.service.TelegramInboundEventMapper;
import com.ca.centranalytics.integration.channel.telegram.service.TelegramWebhookVerificationService;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramWebhookVerificationService telegramWebhookVerificationService;
    private final TelegramInboundEventMapper telegramInboundEventMapper;
    private final IntegrationIngestionService integrationIngestionService;
    private final ObjectMapper objectMapper;

    @PostMapping(path = "${integration.telegram.webhook-path:/api/integrations/webhooks/telegram}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> handleUpdate(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretHeader,
            @RequestBody String payload
    ) {
        telegramWebhookVerificationService.verifySecret(secretHeader);
        integrationIngestionService.ingest(telegramInboundEventMapper.map(readPayload(payload)));
        return Map.of("status", "accepted");
    }

    private JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse Telegram webhook payload", ex);
        }
    }
}
