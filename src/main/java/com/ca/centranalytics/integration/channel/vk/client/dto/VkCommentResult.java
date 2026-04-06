package com.ca.centranalytics.integration.channel.vk.client.dto;

import java.time.Instant;

public record VkCommentResult(
        Long ownerId,
        Long postId,
        Long commentId,
        Long authorVkUserId,
        String text,
        Instant createdAt,
        String rawJson
) {
}
