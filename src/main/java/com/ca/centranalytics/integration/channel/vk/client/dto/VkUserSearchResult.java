package com.ca.centranalytics.integration.channel.vk.client.dto;

import java.time.Instant;

public record VkUserSearchResult(
        Long id,
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
        String rawJson
) {
}
