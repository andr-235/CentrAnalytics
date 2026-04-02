package com.ca.centranalytics.integration.channel.telegram.service;

import com.ca.centranalytics.integration.api.exception.WebhookVerificationException;
import com.ca.centranalytics.integration.channel.telegram.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TelegramWebhookVerificationService {

    private final TelegramProperties telegramProperties;

    public void verifySecret(String secretHeader) {
        if (StringUtils.hasText(telegramProperties.webhookSecret()) && !telegramProperties.webhookSecret().equals(secretHeader)) {
            throw new WebhookVerificationException("Telegram webhook secret is invalid");
        }
    }
}
