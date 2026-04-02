package com.ca.centranalytics.integration.ingestion.dto;

import com.ca.centranalytics.integration.domain.entity.Platform;

public record InboundIntegrationEvent(
        Platform platform,
        String eventType,
        String eventId,
        String rawPayload,
        boolean signatureValid,
        String sourceExternalId,
        String sourceName,
        String sourceSettingsJson,
        InboundConversation conversation,
        InboundAuthor author,
        InboundMessage message
) {
}
