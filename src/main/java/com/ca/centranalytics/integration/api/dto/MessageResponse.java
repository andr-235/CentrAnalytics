package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Platform platform,
        String externalMessageId,
        Long conversationId,
        String conversationTitle,
        String externalConversationId,
        String conversationType,
        Long authorId,
        String authorDisplayName,
        String authorPhone,
        String text,
        MessageType messageType,
        Instant sentAt
) {
}
