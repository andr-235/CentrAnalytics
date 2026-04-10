package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.entity.ConversationType;

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
        String authorUsername,
        String authorExternalUserId,
        String authorPhone,
        String text,
        MessageType messageType,
        Instant sentAt
) {
    public MessageResponse(
            Long id,
            Platform platform,
            String externalMessageId,
            Long conversationId,
            String conversationTitle,
            String externalConversationId,
            ConversationType conversationType,
            Long authorId,
            String authorDisplayName,
            String authorUsername,
            String authorExternalUserId,
            String authorPhone,
            String text,
            MessageType messageType,
            Instant sentAt
    ) {
        this(
                id,
                platform,
                externalMessageId,
                conversationId,
                conversationTitle,
                externalConversationId,
                conversationType == null ? null : conversationType.name(),
                authorId,
                authorDisplayName,
                authorUsername,
                authorExternalUserId,
                authorPhone,
                text,
                messageType,
                sentAt
        );
    }
}
