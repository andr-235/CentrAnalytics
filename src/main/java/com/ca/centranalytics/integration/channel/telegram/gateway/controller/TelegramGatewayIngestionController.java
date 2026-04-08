package com.ca.centranalytics.integration.channel.telegram.gateway.controller;

import com.ca.centranalytics.integration.channel.telegram.gateway.config.TelegramGatewayIngestionProperties;
import com.ca.centranalytics.integration.channel.telegram.gateway.service.TelegramGatewayIngestionService;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@EnableConfigurationProperties(TelegramGatewayIngestionProperties.class)
public class TelegramGatewayIngestionController {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final TelegramGatewayIngestionProperties properties;
    private final TelegramGatewayIngestionService telegramGatewayIngestionService;

    @PostMapping(
            path = "/api/internal/integrations/telegram-user/events",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> ingest(
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @RequestBody InboundIntegrationEvent event
    ) {
        ensureAuthorized(internalToken);
        telegramGatewayIngestionService.ingest(event);
        return Map.of("status", "accepted");
    }

    private void ensureAuthorized(String internalToken) {
        if (!properties.enabled()) {
            throw new IllegalArgumentException("Telegram gateway ingestion is disabled");
        }
        if (!StringUtils.hasText(properties.internalToken())) {
            throw new IllegalArgumentException("Telegram gateway ingestion token is not configured");
        }
        if (!properties.internalToken().equals(internalToken)) {
            throw new IllegalArgumentException("Invalid internal token");
        }
    }
}
