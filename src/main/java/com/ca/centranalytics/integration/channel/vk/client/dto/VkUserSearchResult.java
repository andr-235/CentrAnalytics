package com.ca.centranalytics.integration.channel.vk.client.dto;

public record VkUserSearchResult(
        Long id,
        String displayName,
        String firstName,
        String lastName,
        String profileUrl,
        String city,
        String rawJson
) {
}
