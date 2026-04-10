package com.ca.centranalytics.integration.api.dto;

import java.util.List;

public record PlatformOverviewResponse(
        String platform,
        String label,
        String status,
        List<OverviewHighlightResponse> highlights,
        List<OverviewTrendPointResponse> trend,
        PlatformIntegrationStatusResponse integration,
        List<PlatformAttentionItemResponse> attentionItems
) {
}
