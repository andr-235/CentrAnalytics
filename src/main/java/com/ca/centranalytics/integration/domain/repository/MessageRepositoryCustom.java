package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.api.dto.MessageResponse;
import com.ca.centranalytics.integration.domain.entity.Platform;

import java.time.Instant;
import java.util.List;

public interface MessageRepositoryCustom {
    List<MessageResponse> findMessageResponses(
            Platform platform,
            Long conversationId,
            Long authorId,
            Instant from,
            Instant to,
            String normalizedSearch,
            int offset,
            int limit
    );
}
