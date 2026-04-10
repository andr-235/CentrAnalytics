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
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageQueryService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;

    public List<MessageResponse> getMessages(
            Platform platform,
            Long conversationId,
            Long authorId,
            Instant from,
            Instant to,
            String search,
            Integer limit,
            Integer offset
    ) {
        String normalizedSearch = search == null ? null : search.toLowerCase();
        int resolvedLimit = normalizeLimit(limit);
        int resolvedOffset = normalizeOffset(offset);

        return messageRepository.findMessageResponses(
                platform,
                conversationId,
                authorId,
                from,
                to,
                normalizedSearch,
                resolvedOffset,
                resolvedLimit
        );
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

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private int normalizeOffset(Integer offset) {
        if (offset == null) {
            return 0;
        }

        return Math.max(offset, 0);
    }
}
