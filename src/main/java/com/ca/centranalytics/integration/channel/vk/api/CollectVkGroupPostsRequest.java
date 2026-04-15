package com.ca.centranalytics.integration.channel.vk.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CollectVkGroupPostsRequest(
        @Min(value = 1, message = "limit must be greater than 0")
        @Max(value = 1000, message = "limit must be less than or equal to 1000")
        Integer limit
) {
}
