package com.ca.centranalytics.integration.channel.telegram.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record TelegramWebhookRequest(
        JsonNode payload
) {
}
