package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Platform platform,
        String externalMessageId,
        Long conversationId,
        Long authorId,
        String text,
        MessageType messageType,
        Instant sentAt
) {
}
