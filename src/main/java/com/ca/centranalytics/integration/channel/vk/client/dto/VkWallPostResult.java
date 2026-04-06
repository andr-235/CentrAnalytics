package com.ca.centranalytics.integration.channel.vk.client.dto;

import java.time.Instant;

public record VkWallPostResult(
        Long ownerId,
        Long postId,
        Long authorVkUserId,
        String text,
        Instant createdAt,
        String rawJson
) {
}
