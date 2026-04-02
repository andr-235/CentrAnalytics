package com.ca.centranalytics.integration.channel.vk.controller;

import com.ca.centranalytics.integration.channel.vk.dto.VkCallbackRequest;
import com.ca.centranalytics.integration.channel.vk.service.VkCallbackVerificationService;
import com.ca.centranalytics.integration.channel.vk.service.VkInboundEventMapper;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VkWebhookController {

    private final VkCallbackVerificationService vkCallbackVerificationService;
    private final VkInboundEventMapper vkInboundEventMapper;
    private final IntegrationIngestionService integrationIngestionService;
    private final ObjectMapper objectMapper;

    @PostMapping(path = "${integration.vk.webhook-path:/api/integrations/webhooks/vk}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String handleCallback(@RequestBody String payload) {
        VkCallbackRequest request = readPayload(payload);
        if (vkCallbackVerificationService.isConfirmation(request)) {
            return vkCallbackVerificationService.confirmationCode();
        }

        vkCallbackVerificationService.verify(request);
        if ("message_new".equals(request.type())) {
            integrationIngestionService.ingest(vkInboundEventMapper.map(request));
        }
        return "ok";
    }

    private VkCallbackRequest readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, VkCallbackRequest.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse VK callback payload", ex);
        }
    }
}
