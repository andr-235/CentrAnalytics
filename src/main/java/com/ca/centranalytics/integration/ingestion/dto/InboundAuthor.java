package com.ca.centranalytics.integration.ingestion.dto;

public record InboundAuthor(
        String externalUserId,
        String displayName,
        String username,
        String firstName,
        String lastName,
        String phone,
        String profileUrl,
        boolean bot,
        String metadataJson
) {
}
