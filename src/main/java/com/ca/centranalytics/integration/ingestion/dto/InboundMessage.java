package com.ca.centranalytics.integration.ingestion.dto;

import com.ca.centranalytics.integration.domain.entity.MessageType;

import java.time.Instant;
import java.util.List;

public record InboundMessage(
        String externalMessageId,
        Instant sentAt,
        String text,
        String normalizedText,
        MessageType messageType,
        String replyToExternalMessageId,
        String forwardedFrom,
        List<InboundAttachment> attachments
) {
}
