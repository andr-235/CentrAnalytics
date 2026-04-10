package com.ca.centranalytics.integration.api.dto;

import java.time.Instant;

public record OverviewTrendPointResponse(
        Instant timestamp,
        long messageCount
) {
}
