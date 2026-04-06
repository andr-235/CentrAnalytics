package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.Conversation;
import com.ca.centranalytics.integration.domain.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findBySourceIdAndExternalConversationId(Long sourceId, String externalConversationId);
    List<Conversation> findByPlatform(Platform platform);
}
