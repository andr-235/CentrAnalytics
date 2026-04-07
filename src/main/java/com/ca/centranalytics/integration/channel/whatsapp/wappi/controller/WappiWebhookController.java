package com.ca.centranalytics.integration.channel.whatsapp.wappi.controller;

import com.ca.centranalytics.integration.channel.whatsapp.wappi.client.dto.WappiWebhookPayload;
import com.ca.centranalytics.integration.channel.whatsapp.wappi.service.WappiInboundEventMapper;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WappiWebhookController {

    private final WappiInboundEventMapper wappiInboundEventMapper;
    private final IntegrationIngestionService integrationIngestionService;

    @PostMapping(path = "${integration.wappi.webhook-path:/api/integrations/webhooks/wappi}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> handleWebhook(@RequestBody WappiWebhookPayload payload) {
        List<com.ca.centranalytics.integration.channel.whatsapp.wappi.client.dto.WappiMessageDto> messages =
                payload.messages() == null ? List.of() : payload.messages();
        messages.forEach(message -> integrationIngestionService.ingest(wappiInboundEventMapper.map(message)));
        int processed = messages.size();
        return Map.of("status", "accepted", "processed", processed);
    }
}
