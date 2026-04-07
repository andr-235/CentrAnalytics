package com.ca.centranalytics.integration.channel.telegram.authgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "integration.telegram-auth-gateway")
public record TelegramAuthGatewayProperties(
        boolean enabled,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
