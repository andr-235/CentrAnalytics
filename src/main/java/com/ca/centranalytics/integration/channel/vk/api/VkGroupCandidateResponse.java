package com.ca.centranalytics.integration.channel.vk.api;

import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;

import java.time.Instant;

public record VkGroupCandidateResponse(
        Long id,
        Long vkGroupId,
        Long sourceId,
        String screenName,
        String name,
        VkMatchSource regionMatchSource,
        VkCollectionMethod collectionMethod,
        String rawJson,
        Instant updatedAt
) {
}
