package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;

import java.time.Instant;
import java.util.List;

public record MessageDetailsResponse(
        Long id,
        Platform platform,
        String externalMessageId,
        Long conversationId,
        String conversationTitle,
        Long authorId,
        String authorDisplayName,
        String text,
        String normalizedText,
        MessageType messageType,
        String replyToExternalMessageId,
        String forwardedFrom,
        Instant sentAt,
        List<MessageAttachmentResponse> attachments
) {
    public record MessageAttachmentResponse(
            Long id,
            String attachmentType,
            String externalAttachmentId,
            String url,
            String mimeType
    ) {
    }
}
