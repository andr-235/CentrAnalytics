package com.ca.centranalytics.integration.channel.telegram.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.telegram-gateway-ingestion")
public record TelegramGatewayIngestionProperties(
        boolean enabled,
        String internalToken
) {
}
