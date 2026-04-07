package com.ca.centranalytics.integration.channel.vk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.vk.auto-collection")
public record VkAutoCollectionProperties(
        boolean enabled,
        String region,
        int groupSearchLimit,
        int postLimit,
        int commentPostLimit,
        int commentLimit,
        String collectionMode,
        long fixedDelayMs
) {
}
