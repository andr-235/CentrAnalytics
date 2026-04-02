package com.ca.centranalytics.integration.channel.vk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record VkCallbackRequest(
        String type,
        String secret,
        @JsonProperty("group_id") Long groupId,
        @JsonProperty("event_id") String eventId,
        JsonNode object
) {
}
