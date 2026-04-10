package com.ca.centranalytics.integration.api.dto;

import java.time.Instant;
import java.util.List;

public record OverviewResponse(
        Instant generatedAt,
        String window,
        OverviewSummaryResponse summary,
        List<PlatformOverviewResponse> platforms
) {
}
