package com.ca.centranalytics.integration.channel.vk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.vk")
public record VkProperties(
        long groupId,
        String secret,
        String confirmationCode,
        String accessToken,
        String webhookPath
) {
}
