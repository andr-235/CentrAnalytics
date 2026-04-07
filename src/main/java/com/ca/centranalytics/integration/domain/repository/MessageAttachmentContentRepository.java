package com.ca.centranalytics.integration.domain.repository;

import com.ca.centranalytics.integration.domain.entity.MessageAttachmentContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageAttachmentContentRepository extends JpaRepository<MessageAttachmentContent, Long> {
    Optional<MessageAttachmentContent> findByAttachmentId(Long attachmentId);
}
