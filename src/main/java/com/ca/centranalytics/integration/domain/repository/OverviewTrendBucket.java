package com.ca.centranalytics.integration.domain.repository;

import java.time.Instant;

public record OverviewTrendBucket(
        Instant timestamp,
        long messageCount
) {
}
