package com.ca.centranalytics.integration.channel.vk.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record EnrichVkUsersRequest(
        @NotEmpty(message = "userIds must not be empty")
        List<@Positive(message = "userIds must contain positive values") Long> userIds
) {
}
