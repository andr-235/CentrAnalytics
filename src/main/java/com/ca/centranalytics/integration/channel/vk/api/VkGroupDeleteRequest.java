package com.ca.centranalytics.integration.channel.vk.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record VkGroupDeleteRequest(
        @NotEmpty(message = "groupIdentifiers must not be empty")
        List<String> groupIdentifiers
) {
}
