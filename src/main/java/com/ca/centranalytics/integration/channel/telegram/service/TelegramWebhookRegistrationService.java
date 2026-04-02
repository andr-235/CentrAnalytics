package com.ca.centranalytics.integration.channel.telegram.service;

import com.ca.centranalytics.integration.api.dto.WebhookRegistrationResponse;
import com.ca.centranalytics.integration.channel.telegram.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TelegramWebhookRegistrationService {

    private final TelegramProperties telegramProperties;

    public WebhookRegistrationResponse registerWebhook() {
        boolean configured = StringUtils.hasText(telegramProperties.botToken())
                && StringUtils.hasText(telegramProperties.webhookPath());
        return new WebhookRegistrationResponse(
                "TELEGRAM",
                configured,
                telegramProperties.webhookPath(),
                configured ? "Telegram webhook configuration is ready" : "Telegram webhook configuration is incomplete"
        );
    }
}
