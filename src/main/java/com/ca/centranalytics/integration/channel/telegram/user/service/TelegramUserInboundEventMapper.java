package com.ca.centranalytics.integration.channel.telegram.user.service;

import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;
import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.ingestion.dto.InboundAttachment;
import com.ca.centranalytics.integration.ingestion.dto.InboundAuthor;
import com.ca.centranalytics.integration.ingestion.dto.InboundConversation;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.dto.InboundMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramUserInboundEventMapper {

    private final ObjectMapper objectMapper;

    public InboundIntegrationEvent map(
            TelegramUserSession session,
            TdApi.Chat chat,
            TdApi.Message message,
            TdApi.User senderUser,
            TdApi.Chat senderChat,
            String eventType
    ) {
        if (chat == null || message == null) {
            throw new IllegalArgumentException("Telegram TDLib message context is incomplete");
        }

        String text = extractText(message.content);
        String eventId = buildEventId(eventType, chat.id, message.id, message.editDate);

        return new InboundIntegrationEvent(
                Platform.TELEGRAM,
                eventType,
                eventId,
                writePayload(chat, message, senderUser, senderChat, eventType, session),
                true,
                sourceExternalId(session),
                sourceName(session),
                sourceSettingsJson(session),
                new InboundConversation(
                        String.valueOf(chat.id),
                        mapConversationType(chat.type),
                        chat.title,
                        writeValue(Map.of(
                                "chatId", chat.id,
                                "chatType", chat.type.getClass().getSimpleName()
                        ))
                ),
                mapAuthor(senderUser, senderChat),
                new InboundMessage(
                        String.valueOf(message.id),
                        Instant.ofEpochSecond(message.editDate > 0 ? message.editDate : message.date),
                        text,
                        text == null ? null : text.toLowerCase(),
                        mapMessageType(message.content),
                        extractReplyToMessageId(message.replyTo),
                        null,
                        extractAttachments(message.content)
                )
        );
    }

    private InboundAuthor mapAuthor(TdApi.User senderUser, TdApi.Chat senderChat) {
        if (senderUser != null) {
            return new InboundAuthor(
                    String.valueOf(senderUser.id),
                    buildDisplayName(senderUser),
                    extractUsername(senderUser),
                    senderUser.firstName,
                    senderUser.lastName,
                    senderUser.phoneNumber,
                    null,
                    false,
                    writeValue(Map.of("source", "user"))
            );
        }
        if (senderChat != null) {
            return new InboundAuthor(
                    "chat:" + senderChat.id,
                    senderChat.title,
                    null,
                    senderChat.title,
                    null,
                    null,
                    null,
                    false,
                    writeValue(Map.of("source", "chat", "chatId", senderChat.id))
            );
        }
        return null;
    }

    private ConversationType mapConversationType(TdApi.ChatType chatType) {
        if (chatType instanceof TdApi.ChatTypePrivate) {
            return ConversationType.DIRECT;
        }
        if (chatType instanceof TdApi.ChatTypeSupergroup supergroup) {
            return supergroup.isChannel ? ConversationType.CHANNEL : ConversationType.GROUP;
        }
        if (chatType instanceof TdApi.ChatTypeBasicGroup) {
            return ConversationType.GROUP;
        }
        return ConversationType.GROUP;
    }

    private MessageType mapMessageType(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText) {
            return MessageType.TEXT;
        }
        if (content instanceof TdApi.MessagePhoto) {
            return MessageType.PHOTO;
        }
        if (content instanceof TdApi.MessageDocument) {
            return MessageType.DOCUMENT;
        }
        return MessageType.UNKNOWN;
    }

    private String extractText(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText messageText) {
            return messageText.text == null ? null : messageText.text.text;
        }
        if (content instanceof TdApi.MessagePhoto messagePhoto) {
            return messagePhoto.caption == null ? null : messagePhoto.caption.text;
        }
        if (content instanceof TdApi.MessageDocument messageDocument) {
            return messageDocument.caption == null ? null : messageDocument.caption.text;
        }
        return null;
    }

    private List<InboundAttachment> extractAttachments(TdApi.MessageContent content) {
        List<InboundAttachment> attachments = new ArrayList<>();
        if (content instanceof TdApi.MessagePhoto messagePhoto) {
            String attachmentId = null;
            if (messagePhoto.photo != null && messagePhoto.photo.sizes != null && messagePhoto.photo.sizes.length > 0) {
                TdApi.PhotoSize lastSize = messagePhoto.photo.sizes[messagePhoto.photo.sizes.length - 1];
                if (lastSize.photo != null && lastSize.photo.remote != null) {
                    attachmentId = lastSize.photo.remote.id;
                }
            }
            attachments.add(new InboundAttachment("photo", attachmentId, null, "image/jpeg", "{}"));
        }
        if (content instanceof TdApi.MessageDocument messageDocument) {
            String attachmentId = null;
            String mimeType = null;
            if (messageDocument.document != null) {
                mimeType = messageDocument.document.mimeType;
                if (messageDocument.document.document != null && messageDocument.document.document.remote != null) {
                    attachmentId = messageDocument.document.document.remote.id;
                }
            }
            attachments.add(new InboundAttachment("document", attachmentId, null, mimeType, "{}"));
        }
        return attachments;
    }

    private String extractReplyToMessageId(TdApi.MessageReplyTo replyTo) {
        if (replyTo instanceof TdApi.MessageReplyToMessage replyToMessage) {
            return String.valueOf(replyToMessage.messageId);
        }
        return null;
    }

    private String buildDisplayName(TdApi.User user) {
        String firstName = user.firstName == null ? "" : user.firstName;
        String lastName = user.lastName == null ? "" : user.lastName;
        String displayName = (firstName + " " + lastName).trim();
        return displayName.isBlank() ? extractUsername(user) : displayName;
    }

    private String extractUsername(TdApi.User user) {
        if (user.usernames == null) {
            return null;
        }
        if (user.usernames.editableUsername != null && !user.usernames.editableUsername.isBlank()) {
            return user.usernames.editableUsername;
        }
        if (user.usernames.activeUsernames != null && user.usernames.activeUsernames.length > 0) {
            return user.usernames.activeUsernames[0];
        }
        return null;
    }

    private String sourceExternalId(TelegramUserSession session) {
        return session.getTelegramUserId() != null
                ? String.valueOf(session.getTelegramUserId())
                : "session-" + session.getId();
    }

    private String sourceName(TelegramUserSession session) {
        return "Telegram user " + session.getPhoneNumber();
    }

    private String sourceSettingsJson(TelegramUserSession session) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("mode", "user");
        settings.put("sessionId", session.getId());
        settings.put("phoneNumber", session.getPhoneNumber());
        if (session.getTelegramUserId() != null) {
            settings.put("telegramUserId", session.getTelegramUserId());
        }
        return writeValue(settings);
    }

    private String buildEventId(String eventType, long chatId, long messageId, int editDate) {
        String suffix = editDate > 0 ? "-" + editDate : "";
        return "telegram-user-" + eventType + "-" + chatId + "-" + messageId + suffix;
    }

    private String writePayload(
            TdApi.Chat chat,
            TdApi.Message message,
            TdApi.User senderUser,
            TdApi.Chat senderChat,
            String eventType,
            TelegramUserSession session
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("sessionId", session.getId());
        payload.put("chatId", chat.id);
        payload.put("chatTitle", chat.title);
        payload.put("messageId", message.id);
        payload.put("date", message.date);
        payload.put("editDate", message.editDate);
        payload.put("replyToMessageId", extractReplyToMessageId(message.replyTo));
        payload.put("contentType", message.content == null ? null : message.content.getClass().getSimpleName());
        payload.put("text", extractText(message.content));
        if (senderUser != null) {
            payload.put("senderUserId", senderUser.id);
            payload.put("senderUsername", extractUsername(senderUser));
        }
        if (senderChat != null) {
            payload.put("senderChatId", senderChat.id);
            payload.put("senderChatTitle", senderChat.title);
        }
        return writeValue(payload);
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize Telegram TDLib payload", e);
        }
    }
}
