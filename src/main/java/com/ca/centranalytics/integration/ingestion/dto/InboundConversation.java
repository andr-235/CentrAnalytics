package com.ca.centranalytics.integration.ingestion.dto;

import com.ca.centranalytics.integration.domain.entity.ConversationType;

public record InboundConversation(
        String externalConversationId,
        ConversationType type,
        String title,
        String metadataJson
) {
}
