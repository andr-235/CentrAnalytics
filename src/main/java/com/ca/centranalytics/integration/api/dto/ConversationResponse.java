package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.Platform;

public record ConversationResponse(
        Long id,
        Long sourceId,
        Platform platform,
        String externalConversationId,
        ConversationType type,
        String title
) {
}
