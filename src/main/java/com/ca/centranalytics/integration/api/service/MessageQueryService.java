package com.ca.centranalytics.integration.api.service;

import com.ca.centranalytics.integration.api.dto.MessageDetailsResponse;
import com.ca.centranalytics.integration.api.dto.MessageResponse;
import com.ca.centranalytics.integration.api.exception.IntegrationNotFoundException;
import com.ca.centranalytics.integration.domain.entity.Message;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.repository.MessageAttachmentRepository;
import com.ca.centranalytics.integration.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageQueryService {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;

    public List<MessageResponse> getMessages(Platform platform, Long conversationId, Long authorId, Instant from, Instant to, String search) {
        String normalizedSearch = search == null ? null : search.toLowerCase();
        return messageRepository.findAll().stream()
                .filter(message -> platform == null || message.getPlatform() == platform)
                .filter(message -> conversationId == null || message.getConversation().getId().equals(conversationId))
                .filter(message -> authorId == null || (message.getAuthor() != null && message.getAuthor().getId().equals(authorId)))
                .filter(message -> from == null || !message.getSentAt().isBefore(from))
                .filter(message -> to == null || !message.getSentAt().isAfter(to))
                .filter(message -> normalizedSearch == null
                        || contains(message.getText(), normalizedSearch)
                        || contains(message.getNormalizedText(), normalizedSearch))
                .sorted(Comparator.comparing(Message::getSentAt).reversed())
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getPlatform(),
                        message.getExternalMessageId(),
                        message.getConversation().getId(),
                        message.getAuthor() == null ? null : message.getAuthor().getId(),
                        message.getText(),
                        message.getMessageType(),
                        message.getSentAt()
                ))
                .toList();
    }

    public MessageDetailsResponse getMessage(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new IntegrationNotFoundException("Message not found: " + id));

        return new MessageDetailsResponse(
                message.getId(),
                message.getPlatform(),
                message.getExternalMessageId(),
                message.getConversation().getId(),
                message.getConversation().getTitle(),
                message.getAuthor() == null ? null : message.getAuthor().getId(),
                message.getAuthor() == null ? null : message.getAuthor().getDisplayName(),
                message.getText(),
                message.getNormalizedText(),
                message.getMessageType(),
                message.getReplyToExternalMessageId(),
                message.getForwardedFrom(),
                message.getSentAt(),
                messageAttachmentRepository.findByMessageId(message.getId()).stream()
                        .map(attachment -> new MessageDetailsResponse.MessageAttachmentResponse(
                                attachment.getId(),
                                attachment.getAttachmentType(),
                                attachment.getExternalAttachmentId(),
                                attachment.getUrl(),
                                attachment.getMimeType()
                        ))
                        .toList()
        );
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }
}
