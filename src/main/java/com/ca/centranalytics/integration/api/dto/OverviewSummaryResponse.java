package com.ca.centranalytics.integration.api.dto;

public record OverviewSummaryResponse(
        long messageCount,
        long conversationCount,
        long activeAuthorCount,
        long platformIssueCount
) {
}
