package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.entity.ProcessingStatus;

import java.time.Instant;

public record RawEventResponse(
        Long id,
        Platform platform,
        String eventType,
        String eventId,
        boolean signatureValid,
        ProcessingStatus processingStatus,
        String payloadJson,
        String errorMessage,
        Instant receivedAt
) {
}
