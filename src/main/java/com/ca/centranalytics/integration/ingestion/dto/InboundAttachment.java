package com.ca.centranalytics.integration.ingestion.dto;

public record InboundAttachment(
        String attachmentType,
        String externalAttachmentId,
        String url,
        String mimeType,
        String metadataJson,
        String fileName,
        String contentBase64
) {
    public InboundAttachment(
            String attachmentType,
            String externalAttachmentId,
            String url,
            String mimeType,
            String metadataJson
    ) {
        this(attachmentType, externalAttachmentId, url, mimeType, metadataJson, null, null);
    }
}
