package com.ca.centranalytics.integration.api.dto;

public record WebhookRegistrationResponse(
        String platform,
        boolean configured,
        String webhookPath,
        String message
) {
}
