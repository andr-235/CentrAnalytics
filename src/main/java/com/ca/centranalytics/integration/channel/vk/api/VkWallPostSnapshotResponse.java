package com.ca.centranalytics.integration.channel.vk.api;

import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;

import java.time.Instant;

public record VkWallPostSnapshotResponse(
        Long id,
        Long ownerId,
        Long postId,
        Long sourceId,
        Long authorVkUserId,
        String text,
        VkCollectionMethod collectionMethod,
        String rawJson,
        Instant updatedAt
) {
}
