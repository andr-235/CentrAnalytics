package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.Message;
import com.ca.centranalytics.integration.domain.entity.Platform;

import java.time.Instant;
import java.util.List;

public interface MessageRepositoryCustom {
    List<Message> findMessages(
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
