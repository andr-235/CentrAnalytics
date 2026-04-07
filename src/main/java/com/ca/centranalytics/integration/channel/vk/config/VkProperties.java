package com.ca.centranalytics.integration.channel.vk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "integration.vk")
public record VkProperties(
        long groupId,
        String accessToken,
        String userAccessToken,
        String apiVersion,
        String apiBaseUrl,
        String fallbackBaseUrl,
        Duration requestTimeout
) {
}
