package com.ca.centranalytics.integration.api.dto;

import java.time.Instant;

public record PlatformIntegrationStatusResponse(
        String syncStatus,
        Instant lastEventAt,
        Instant lastSuccessAt,
        String lastErrorMessage,
        long sourceCount,
        String detail
) {
}
