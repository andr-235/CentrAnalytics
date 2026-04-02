package com.ca.centranalytics.integration.channel.telegram.service;

import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.ingestion.dto.InboundAttachment;
import com.ca.centranalytics.integration.ingestion.dto.InboundAuthor;
import com.ca.centranalytics.integration.ingestion.dto.InboundConversation;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.dto.InboundMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramInboundEventMapper {

    private final ObjectMapper objectMapper;

    public InboundIntegrationEvent map(JsonNode payload) {
        Update update = objectMapper.convertValue(payload, Update.class);
        Message message = update.getMessage() != null ? update.getMessage() : update.getEditedMessage();
        if (message == null) {
            throw new IllegalArgumentException("Telegram update does not contain a supported message");
        }

        Chat chat = message.getChat();
        User from = message.getFrom();
        String text = message.getText() != null ? message.getText() : message.getCaption();

        return new InboundIntegrationEvent(
                Platform.TELEGRAM,
                "message",
                "telegram-" + chat.getId() + "-" + message.getMessageId(),
                writeValue(payload),
                true,
                String.valueOf(chat.getId()),
                chat.getTitle() != null ? chat.getTitle() : String.valueOf(chat.getId()),
                "{\"chatId\":" + chat.getId() + "}",
                new InboundConversation(
                        String.valueOf(chat.getId()),
                        mapConversationType(chat.getType()),
                        chat.getTitle() != null ? chat.getTitle() : chat.getUserName(),
                        "{\"chatType\":\"" + chat.getType() + "\"}"
                ),
                from == null ? null : new InboundAuthor(
                        String.valueOf(from.getId()),
                        buildDisplayName(from),
                        from.getUserName(),
                        from.getFirstName(),
                        from.getLastName(),
                        null,
                        null,
                        Boolean.TRUE.equals(from.getIsBot()),
                        "{}"
                ),
                new InboundMessage(
                        String.valueOf(message.getMessageId()),
                        Instant.ofEpochSecond(message.getDate()),
                        text,
                        text == null ? null : text.toLowerCase(),
                        mapMessageType(message),
                        message.getReplyToMessage() == null ? null : String.valueOf(message.getReplyToMessage().getMessageId()),
                        null,
                        extractAttachments(message)
                )
        );
    }

    private List<InboundAttachment> extractAttachments(Message message) {
        List<InboundAttachment> attachments = new ArrayList<>();
        if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
            attachments.add(new InboundAttachment("photo", photo.getFileId(), null, "image/jpeg", "{}"));
        }
        if (message.getDocument() != null) {
            Document document = message.getDocument();
            attachments.add(new InboundAttachment("document", document.getFileId(), null, document.getMimeType(), "{}"));
        }
        return attachments;
    }

    private MessageType mapMessageType(Message message) {
        if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            return MessageType.PHOTO;
        }
        if (message.getDocument() != null) {
            return MessageType.DOCUMENT;
        }
        return MessageType.TEXT;
    }

    private ConversationType mapConversationType(String type) {
        return switch (type) {
            case "private" -> ConversationType.DIRECT;
            case "channel" -> ConversationType.CHANNEL;
            default -> ConversationType.GROUP;
        };
    }

    private String buildDisplayName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName();
        String lastName = user.getLastName() == null ? "" : user.getLastName();
        String displayName = (firstName + " " + lastName).trim();
        return displayName.isEmpty() ? user.getUserName() : displayName;
    }

    private String writeValue(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize Telegram update payload", e);
        }
    }
}
