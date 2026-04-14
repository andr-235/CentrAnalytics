package com.ca.centranalytics.integration.ingestion.service;

import com.ca.centranalytics.integration.channel.whatsapp.wappi.service.WappiAttachmentContentService;
import com.ca.centranalytics.integration.domain.entity.Conversation;
import com.ca.centranalytics.integration.domain.entity.ExternalUser;
import com.ca.centranalytics.integration.domain.entity.Message;
import com.ca.centranalytics.integration.domain.entity.MessageAttachment;
import com.ca.centranalytics.integration.domain.entity.ProcessingStatus;
import com.ca.centranalytics.integration.domain.entity.RawEvent;
import com.ca.centranalytics.integration.domain.repository.MessageAttachmentContentRepository;
import com.ca.centranalytics.integration.domain.repository.MessageAttachmentRepository;
import com.ca.centranalytics.integration.domain.repository.MessageRepository;
import com.ca.centranalytics.integration.ingestion.dto.InboundAttachment;
import com.ca.centranalytics.integration.ingestion.dto.InboundMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessagePersistenceService {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageAttachmentContentRepository messageAttachmentContentRepository;
    private final WappiAttachmentContentService wappiAttachmentContentService;

    @Transactional
    public Message persist(Conversation conversation, ExternalUser author, RawEvent rawEvent, InboundMessage inboundMessage) {
        validate(inboundMessage);

        Message message = messageRepository.findByConversationIdAndExternalMessageId(conversation.getId(), inboundMessage.externalMessageId())
                .orElseGet(() -> Message.builder()
                        .conversation(conversation)
                        .platform(conversation.getPlatform())
                        .externalMessageId(inboundMessage.externalMessageId())
                        .build());

        message.setConversation(conversation);
        message.setAuthor(author);
        message.setSentAt(inboundMessage.sentAt());
        message.setText(inboundMessage.text());
        message.setNormalizedText(inboundMessage.normalizedText());
        message.setMessageType(inboundMessage.messageType());
        message.setReplyToExternalMessageId(inboundMessage.replyToExternalMessageId());
        message.setForwardedFrom(inboundMessage.forwardedFrom());
        message.setHasAttachments(inboundMessage.attachments() != null && !inboundMessage.attachments().isEmpty());
        message.setRawEvent(rawEvent);
        message.setIngestionStatus(ProcessingStatus.PERSISTED);
        Message savedMessage = messageRepository.save(message);

        syncAttachments(savedMessage, inboundMessage.attachments());

        return savedMessage;
    }

    private void syncAttachments(Message message, List<InboundAttachment> newAttachments) {
        List<MessageAttachment> existingAttachments = messageAttachmentRepository.findByMessageId(message.getId());

        if (newAttachments == null || newAttachments.isEmpty()) {
            deleteAllIfExists(existingAttachments);
            return;
        }

        Set<String> newAttachmentIds = newAttachments.stream()
                .map(InboundAttachment::externalAttachmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        AttachmentIndexes indexes = buildAttachmentIndexes(existingAttachments);

        processNewAttachments(message, newAttachments, indexes);
        deleteOrphanedAttachments(existingAttachments, newAttachmentIds, indexes);
    }

    private void deleteAllIfExists(List<MessageAttachment> attachments) {
        if (!attachments.isEmpty()) {
            messageAttachmentRepository.deleteAll(attachments);
        }
    }

    private AttachmentIndexes buildAttachmentIndexes(List<MessageAttachment> existingAttachments) {
        Map<String, MessageAttachment> byExternalId = existingAttachments.stream()
                .filter(a -> a.getExternalAttachmentId() != null)
                .collect(Collectors.toMap(MessageAttachment::getExternalAttachmentId, a -> a));

        Map<String, MessageAttachment> byUrl = existingAttachments.stream()
                .filter(a -> a.getUrl() != null)
                .collect(Collectors.toMap(MessageAttachment::getUrl, a -> a, (a, b) -> a));

        return new AttachmentIndexes(byExternalId, byUrl);
    }

    private void processNewAttachments(
            Message message,
            List<InboundAttachment> newAttachments,
            AttachmentIndexes indexes
    ) {
        for (InboundAttachment newAttachment : newAttachments) {
            MessageAttachment existing = findExistingAttachment(newAttachment, indexes);

            if (existing != null) {
                updateExistingAttachment(existing, newAttachment);
            } else {
                createNewAttachment(message, newAttachment);
            }
        }
    }

    private MessageAttachment findExistingAttachment(InboundAttachment attachment, AttachmentIndexes indexes) {
        String externalId = attachment.externalAttachmentId();
        String url = attachment.url();

        if (externalId != null && indexes.byExternalId().containsKey(externalId)) {
            return indexes.byExternalId().get(externalId);
        }
        if (url != null && indexes.byUrl().containsKey(url)) {
            return indexes.byUrl().get(url);
        }
        return null;
    }

    private void updateExistingAttachment(MessageAttachment existing, InboundAttachment newAttachment) {
        existing.setAttachmentType(newAttachment.attachmentType());
        existing.setUrl(newAttachment.url());
        existing.setMimeType(newAttachment.mimeType());
        existing.setMetadataJson(newAttachment.metadataJson());
        messageAttachmentRepository.save(existing);

        wappiAttachmentContentService.buildContent(existing, newAttachment)
                .ifPresent(messageAttachmentContentRepository::save);
    }

    private void createNewAttachment(Message message, InboundAttachment attachment) {
        MessageAttachment newAttachmentEntity = toAttachment(message, attachment);
        MessageAttachment savedAttachment = messageAttachmentRepository.save(newAttachmentEntity);

        wappiAttachmentContentService.buildContent(savedAttachment, attachment)
                .ifPresent(messageAttachmentContentRepository::save);
    }

    private void deleteOrphanedAttachments(
            List<MessageAttachment> existingAttachments,
            Set<String> newAttachmentIds,
            AttachmentIndexes indexes
    ) {
        List<MessageAttachment> orphanedAttachments = existingAttachments.stream()
                .filter(a -> isOrphaned(a, newAttachmentIds, indexes))
                .toList();

        if (!orphanedAttachments.isEmpty()) {
            messageAttachmentRepository.deleteAll(orphanedAttachments);
        }
    }

    private boolean isOrphaned(
            MessageAttachment attachment,
            Set<String> newAttachmentIds,
            AttachmentIndexes indexes
    ) {
        String externalId = attachment.getExternalAttachmentId();
        String url = attachment.getUrl();

        if (externalId != null) {
            return !newAttachmentIds.contains(externalId);
        }
        if (url != null) {
            return indexes.byUrl().keySet().stream().noneMatch(u -> u.equals(url));
        }
        return true;
    }

    private record AttachmentIndexes(
            Map<String, MessageAttachment> byExternalId,
            Map<String, MessageAttachment> byUrl
    ) {
    }

    private void validate(InboundMessage inboundMessage) {
        if (inboundMessage == null) {
            throw new IllegalArgumentException("Inbound message is required");
        }
        if (!StringUtils.hasText(inboundMessage.externalMessageId())) {
            throw new IllegalArgumentException("Inbound message externalMessageId is required");
        }
        if (inboundMessage.sentAt() == null) {
            throw new IllegalArgumentException("Inbound message sentAt is required");
        }
        if (inboundMessage.messageType() == null) {
            throw new IllegalArgumentException("Inbound message messageType is required");
        }
    }

    private MessageAttachment toAttachment(Message message, InboundAttachment attachment) {
        return MessageAttachment.builder()
                .message(message)
                .attachmentType(attachment.attachmentType())
                .externalAttachmentId(attachment.externalAttachmentId())
                .url(attachment.url())
                .mimeType(attachment.mimeType())
                .metadataJson(attachment.metadataJson())
                .build();
    }
}
