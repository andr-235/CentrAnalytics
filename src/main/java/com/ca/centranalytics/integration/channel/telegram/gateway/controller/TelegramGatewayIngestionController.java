package com.ca.centranalytics.integration.channel.telegram.gateway.controller;

import com.ca.centranalytics.integration.channel.telegram.gateway.service.TelegramGatewayIngestionService;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Ingestion endpoint for Telegram gateway events.
 * Internal token validation is handled by {@link com.ca.centranalytics.auth.security.InternalTokenFilter}.
 */
@RestController
@RequiredArgsConstructor
public class TelegramGatewayIngestionController {

    private final TelegramGatewayIngestionService telegramGatewayIngestionService;

    @PostMapping(
            path = "/api/internal/integrations/telegram-user/events",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> ingest(@RequestBody InboundIntegrationEvent event) {
        telegramGatewayIngestionService.ingest(event);
        return Map.of("status", "accepted");
    }
}
