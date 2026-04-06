package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.Message;
import com.ca.centranalytics.integration.domain.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Optional<Message> findByConversationIdAndExternalMessageId(Long conversationId, String externalMessageId);
    List<Message> findByPlatform(Platform platform);
    List<Message> findByConversationId(Long conversationId);
    List<Message> findByAuthorId(Long authorId);
    List<Message> findBySentAtBetween(Instant from, Instant to);
}
