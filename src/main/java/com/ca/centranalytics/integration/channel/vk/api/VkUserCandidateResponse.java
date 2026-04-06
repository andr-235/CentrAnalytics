package com.ca.centranalytics.integration.channel.vk.api;

import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;

import java.time.Instant;

public record VkUserCandidateResponse(
        Long id,
        Long vkUserId,
        Long sourceId,
        String displayName,
        String firstName,
        String lastName,
        String username,
        String profileUrl,
        String city,
        String homeTown,
        String birthDate,
        Integer sex,
        String status,
        Instant lastSeenAt,
        String avatarUrl,
        String mobilePhone,
        String homePhone,
        String site,
        Integer relation,
        String education,
        String careerJson,
        String countersJson,
        VkMatchSource regionMatchSource,
        VkCollectionMethod collectionMethod,
        String rawJson,
        Instant updatedAt
) {
}
