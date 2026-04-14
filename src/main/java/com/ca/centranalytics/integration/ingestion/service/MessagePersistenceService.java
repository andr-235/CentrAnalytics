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
            if (!existingAttachments.isEmpty()) {
                messageAttachmentRepository.deleteAll(existingAttachments);
            }
            return;
        }

        Set<String> newAttachmentIds = newAttachments.stream()
                .map(InboundAttachment::externalAttachmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, MessageAttachment> existingByExternalId = existingAttachments.stream()
                .filter(a -> a.getExternalAttachmentId() != null)
                .collect(Collectors.toMap(MessageAttachment::getExternalAttachmentId, a -> a));

        Map<String, MessageAttachment> existingByUrl = existingAttachments.stream()
                .filter(a -> a.getUrl() != null)
                .collect(Collectors.toMap(MessageAttachment::getUrl, a -> a, (a, b) -> a));

        for (InboundAttachment newAttachment : newAttachments) {
            String externalId = newAttachment.externalAttachmentId();
            String url = newAttachment.url();

            MessageAttachment existing = null;
            if (externalId != null) {
                existing = existingByExternalId.get(externalId);
            }
            if (existing == null && url != null) {
                existing = existingByUrl.get(url);
            }

            if (existing != null) {
                existing.setAttachmentType(newAttachment.attachmentType());
                existing.setUrl(url);
                existing.setMimeType(newAttachment.mimeType());
                existing.setMetadataJson(newAttachment.metadataJson());
                messageAttachmentRepository.save(existing);

                wappiAttachmentContentService.buildContent(existing, newAttachment)
                        .ifPresent(messageAttachmentContentRepository::save);
            } else {
                MessageAttachment newAttachmentEntity = toAttachment(message, newAttachment);
                MessageAttachment savedAttachment = messageAttachmentRepository.save(newAttachmentEntity);

                wappiAttachmentContentService.buildContent(savedAttachment, newAttachment)
                        .ifPresent(messageAttachmentContentRepository::save);
            }
        }

        List<MessageAttachment> orphanedAttachments = existingAttachments.stream()
                .filter(a -> {
                    if (a.getExternalAttachmentId() != null) {
                        return !newAttachmentIds.contains(a.getExternalAttachmentId());
                    }
                    if (a.getUrl() != null) {
                        return newAttachments.stream().noneMatch(n -> a.getUrl().equals(n.url()));
                    }
                    return true;
                })
                .toList();

        if (!orphanedAttachments.isEmpty()) {
            messageAttachmentRepository.deleteAll(orphanedAttachments);
        }
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
