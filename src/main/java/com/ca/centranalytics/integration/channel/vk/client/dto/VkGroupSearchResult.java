package com.ca.centranalytics.integration.channel.vk.client.dto;

public record VkGroupSearchResult(
        Long id,
        String name,
        String screenName,
        String description,
        String city,
        String rawJson
) {
}
