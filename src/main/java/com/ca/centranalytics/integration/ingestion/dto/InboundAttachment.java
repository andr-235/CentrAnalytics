package com.ca.centranalytics.integration.ingestion.dto;

public record InboundAttachment(
        String attachmentType,
        String externalAttachmentId,
        String url,
        String mimeType,
        String metadataJson
) {
}
