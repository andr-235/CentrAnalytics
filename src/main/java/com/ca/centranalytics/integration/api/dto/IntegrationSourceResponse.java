package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.IntegrationStatus;
import com.ca.centranalytics.integration.domain.entity.Platform;

public record IntegrationSourceResponse(
        Long id,
        Platform platform,
        String name,
        IntegrationStatus status
) {
}
