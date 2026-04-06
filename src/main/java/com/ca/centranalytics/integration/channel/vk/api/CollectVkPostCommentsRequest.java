package com.ca.centranalytics.integration.channel.vk.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CollectVkPostCommentsRequest(
        @NotEmpty(message = "postIds must not be empty")
        List<@Positive(message = "postIds must contain positive values") Long> postIds,
        @Min(value = 1, message = "limit must be greater than 0")
        @Max(value = 1000, message = "limit must be less than or equal to 1000")
        Integer limit,
        @Pattern(regexp = "OFFICIAL_ONLY|HYBRID", message = "collectionMode must be OFFICIAL_ONLY or HYBRID")
        String collectionMode
) {
}
