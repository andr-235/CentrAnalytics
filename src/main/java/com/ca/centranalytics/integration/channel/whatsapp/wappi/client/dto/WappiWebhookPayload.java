package com.ca.centranalytics.integration.channel.whatsapp.wappi.client.dto;

import java.util.List;

public record WappiWebhookPayload(
        List<WappiMessageDto> messages
) {
}
