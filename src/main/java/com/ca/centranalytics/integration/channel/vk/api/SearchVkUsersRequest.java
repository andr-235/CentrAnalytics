package com.ca.centranalytics.integration.channel.vk.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SearchVkUsersRequest(
        @NotBlank(message = "region is required")
        String region,
        @Min(value = 1, message = "limit must be greater than 0")
        @Max(value = 1000, message = "limit must be less than or equal to 1000")
        Integer limit,
        @Pattern(regexp = "OFFICIAL_ONLY|HYBRID", message = "collectionMode must be OFFICIAL_ONLY or HYBRID")
        String collectionMode
) {
}
