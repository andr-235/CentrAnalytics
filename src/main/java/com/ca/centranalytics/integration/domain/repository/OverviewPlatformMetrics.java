package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.Platform;

import java.time.Instant;

public record OverviewPlatformMetrics(
        Platform platform,
        long messageCount,
        long conversationCount,
        long activeAuthorCount,
        Instant lastEventAt
) {
}
