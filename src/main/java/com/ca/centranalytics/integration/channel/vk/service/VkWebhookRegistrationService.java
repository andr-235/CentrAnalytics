package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.api.dto.WebhookRegistrationResponse;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VkWebhookRegistrationService {

    private final VkProperties vkProperties;

    public WebhookRegistrationResponse registerWebhook() {
        boolean configured = vkProperties.groupId() > 0 && StringUtils.hasText(vkProperties.accessToken());
        return new WebhookRegistrationResponse(
                "VK",
                configured,
                vkProperties.webhookPath(),
                configured ? "VK webhook configuration is ready" : "VK webhook configuration is incomplete"
        );
    }
}
