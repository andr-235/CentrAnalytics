package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.api.dto.OverviewWindow;
import com.ca.centranalytics.integration.domain.entity.Platform;

import java.time.Instant;
import java.util.List;

public interface OverviewMetricsRepository {
    OverviewSummaryMetrics fetchSummary(Instant from, Instant to);

    List<OverviewPlatformMetrics> fetchPlatformMetrics(Instant from, Instant to);

    List<OverviewTrendBucket> fetchTrend(Platform platform, Instant from, Instant to, OverviewWindow window);
}
