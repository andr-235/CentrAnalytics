package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.Platform;

public record ExternalUserResponse(
        Long id,
        Platform platform,
        String externalUserId,
        String displayName,
        String username,
        boolean bot
) {
}
