package com.ca.centranalytics.integration.admin.controller;

import com.ca.centranalytics.integration.api.dto.RawEventResponse;
import com.ca.centranalytics.integration.api.dto.WebhookRegistrationResponse;
import com.ca.centranalytics.integration.api.service.IntegrationQueryService;
import com.ca.centranalytics.integration.channel.telegram.service.TelegramWebhookRegistrationService;
import com.ca.centranalytics.integration.channel.vk.service.VkWebhookRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IntegrationAdminController {

    private final IntegrationQueryService integrationQueryService;
    private final VkWebhookRegistrationService vkWebhookRegistrationService;
    private final TelegramWebhookRegistrationService telegramWebhookRegistrationService;

    @GetMapping("/api/raw-events/{id}")
    public RawEventResponse getRawEvent(@PathVariable Long id) {
        return integrationQueryService.getRawEvent(id);
    }

    @PostMapping("/api/admin/integrations/vk/register-webhook")
    public WebhookRegistrationResponse registerVkWebhook() {
        return vkWebhookRegistrationService.registerWebhook();
    }

    @PostMapping("/api/admin/integrations/telegram/register-webhook")
    public WebhookRegistrationResponse registerTelegramWebhook() {
        return telegramWebhookRegistrationService.registerWebhook();
    }
}
