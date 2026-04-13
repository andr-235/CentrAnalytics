package com.ca.centranalytics.integration.channel.max.wappi.controller;

import com.ca.centranalytics.integration.channel.max.wappi.client.dto.MaxMessageDto;
import com.ca.centranalytics.integration.channel.max.wappi.client.dto.MaxWebhookPayload;
import com.ca.centranalytics.integration.channel.max.wappi.service.MaxInboundEventMapper;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MaxWebhookController {

    private final MaxInboundEventMapper maxInboundEventMapper;
    private final IntegrationIngestionService integrationIngestionService;

    @PostMapping(path = "${integration.max.webhook-path:/api/integrations/webhooks/wappi/max}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> handleWebhook(@RequestBody MaxWebhookPayload payload) {
        List<MaxMessageDto> messages = payload.messages() == null ? List.of() : payload.messages();
        int processed = 0;
        for (MaxMessageDto message : messages) {
            if (!isIncomingMessage(message)) {
                continue;
            }
            integrationIngestionService.ingest(maxInboundEventMapper.map(message));
            processed++;
        }
        return Map.of("status", "accepted", "processed", processed);
    }

    private boolean isIncomingMessage(MaxMessageDto message) {
        return !StringUtils.hasText(message.whType()) || "incoming_message".equals(message.whType());
    }
}
