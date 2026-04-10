package com.ca.centranalytics.integration.domain.repository;

public record OverviewSummaryMetrics(
        long messageCount,
        long conversationCount,
        long activeAuthorCount
) {
}
