package com.ca.centranalytics.integration.channel.max.wappi.client.dto;

import java.util.List;

public record MaxWebhookPayload(
        List<MaxMessageDto> messages
) {
}
