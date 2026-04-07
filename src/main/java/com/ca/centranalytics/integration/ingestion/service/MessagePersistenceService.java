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
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessagePersistenceService {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageAttachmentContentRepository messageAttachmentContentRepository;
    private final WappiAttachmentContentService wappiAttachmentContentService;

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

        List<MessageAttachment> existingAttachments = messageAttachmentRepository.findByMessageId(savedMessage.getId());
        if (!existingAttachments.isEmpty()) {
            messageAttachmentRepository.deleteAll(existingAttachments);
        }

        if (inboundMessage.attachments() != null) {
            inboundMessage.attachments().forEach(attachment -> {
                MessageAttachment savedAttachment = messageAttachmentRepository.save(toAttachment(savedMessage, attachment));
                wappiAttachmentContentService.buildContent(savedAttachment, attachment)
                        .ifPresent(messageAttachmentContentRepository::save);
            });
        }

        return savedMessage;
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
