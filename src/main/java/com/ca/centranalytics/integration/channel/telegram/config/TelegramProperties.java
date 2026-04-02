package com.ca.centranalytics.integration.channel.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.telegram")
public record TelegramProperties(
        String botToken,
        String webhookSecret,
        String webhookPath,
        String botUsername
) {
}
