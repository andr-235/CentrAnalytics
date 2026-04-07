package com.ca.centranalytics.integration.channel.telegram.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.telegram-user")
public record TelegramUserProperties(
        boolean enabled,
        int apiId,
        String apiHash,
        String databaseDir,
        String filesDir,
        String systemLanguageCode,
        String deviceModel,
        String systemVersion,
        String applicationVersion,
        boolean proxyEnabled,
        String proxyHost,
        int proxyPort,
        String proxyUsername,
        String proxyPassword
) {
}
