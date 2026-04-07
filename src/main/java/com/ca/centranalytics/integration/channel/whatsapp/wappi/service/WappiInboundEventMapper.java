package com.ca.centranalytics.integration.channel.whatsapp.wappi.service;

import com.ca.centranalytics.integration.channel.whatsapp.wappi.client.dto.WappiMessageDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WappiInboundEventMapper {

    private final ObjectMapper objectMapper;

    public InboundIntegrationEvent map(WappiMessageDto message) {
        String text = resolveText(message);
        return new InboundIntegrationEvent(
                Platform.WAPPI,
                StringUtils.hasText(message.whType()) ? message.whType() : "incoming_message",
                "wappi-" + message.profileId() + "-" + message.id(),
                writeValue(message),
                true,
                message.profileId(),
                "Wappi profile " + message.profileId(),
                writeValue(Map.of("profileId", message.profileId())),
                new InboundConversation(
                        message.chatId(),
                        mapConversationType(message.chatType()),
                        buildConversationTitle(message),
                        writeValue(metadata(
                                "chatType", message.chatType(),
                                "to", message.to()
                        ))
                ),
                new InboundAuthor(
                        message.from(),
                        buildDisplayName(message),
                        null,
                        null,
                        null,
                        extractPhone(message.from()),
                        null,
                        Boolean.TRUE.equals(message.isBot()),
                        writeValue(metadata(
                                "fromWhere", message.fromWhere(),
                                "senderName", message.senderName(),
                                "contactName", message.contactName(),
                                "wappiBotId", message.wappiBotId()
                        ))
                ),
                new InboundMessage(
                        message.id(),
                        parseInstant(message),
                        text,
                        text == null ? null : text.toLowerCase(),
                        mapMessageType(message.type()),
                        message.replyMessage() == null ? null : message.replyMessage().id(),
                        Boolean.TRUE.equals(message.isForwarded()) ? message.from() : null,
                        extractAttachments(message)
                )
        );
    }

    private List<InboundAttachment> extractAttachments(WappiMessageDto message) {
        MessageType messageType = mapMessageType(message.type());
        if (messageType == MessageType.TEXT || messageType == MessageType.UNKNOWN) {
            return List.of();
        }
        return List.of(new InboundAttachment(
                mapAttachmentType(message.type()),
                message.id(),
                resolveAttachmentUrl(message),
                message.mimetype(),
                writeValue(metadata(
                        "caption", message.caption(),
                        "title", message.title(),
                        "chatType", message.chatType(),
                        "thumbnail", message.thumbnail(),
                        "picture", message.picture(),
                        "isDeleted", message.isDeleted()
                )),
                message.fileName(),
                resolveAttachmentContent(message)
        ));
    }

    private MessageType mapMessageType(String type) {
        if (!StringUtils.hasText(type)) {
            return MessageType.UNKNOWN;
        }
        return switch (type) {
            case "chat" -> MessageType.TEXT;
            case "image" -> MessageType.PHOTO;
            case "video" -> MessageType.VIDEO;
            case "document" -> MessageType.DOCUMENT;
            case "ptt", "audio" -> MessageType.AUDIO;
            default -> MessageType.UNKNOWN;
        };
    }

    private String mapAttachmentType(String type) {
        if (!StringUtils.hasText(type)) {
            return "unknown";
        }
        if ("ptt".equals(type)) {
            return "audio";
        }
        return type;
    }

    private ConversationType mapConversationType(String chatType) {
        if ("dialog".equals(chatType)) {
            return ConversationType.DIRECT;
        }
        if ("channel".equals(chatType)) {
            return ConversationType.CHANNEL;
        }
        return ConversationType.GROUP;
    }

    private Instant parseInstant(WappiMessageDto message) {
        if (StringUtils.hasText(message.timestamp())) {
            return OffsetDateTime.parse(message.timestamp()).toInstant();
        }
        if (message.time() != null) {
            return Instant.ofEpochSecond(message.time());
        }
        throw new IllegalArgumentException("Wappi message timestamp is required");
    }

    private String resolveText(WappiMessageDto message) {
        if ("chat".equals(message.type())) {
            return message.body();
        }
        if (StringUtils.hasText(message.caption())) {
            return message.caption();
        }
        if (StringUtils.hasText(message.title())) {
            return message.title();
        }
        return null;
    }

    private String resolveAttachmentUrl(WappiMessageDto message) {
        if (message.body() != null && (message.body().startsWith("http://") || message.body().startsWith("https://"))) {
            return message.body();
        }
        return message.picture();
    }

    private String resolveAttachmentContent(WappiMessageDto message) {
        if (message.body() != null && (message.body().startsWith("http://") || message.body().startsWith("https://"))) {
            return null;
        }
        return message.body();
    }

    private String buildDisplayName(WappiMessageDto message) {
        if (StringUtils.hasText(message.contactName())) {
            return message.contactName();
        }
        if (StringUtils.hasText(message.senderName())) {
            return message.senderName();
        }
        return extractPhone(message.from());
    }

    private String buildConversationTitle(WappiMessageDto message) {
        if (StringUtils.hasText(message.contactName())) {
            return message.contactName();
        }
        if (StringUtils.hasText(message.senderName())) {
            return message.senderName();
        }
        return message.chatId();
    }

    private String extractPhone(String whatsappId) {
        if (!StringUtils.hasText(whatsappId)) {
            return null;
        }
        int atIndex = whatsappId.indexOf('@');
        return atIndex > 0 ? whatsappId.substring(0, atIndex) : whatsappId;
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize Wappi payload", e);
        }
    }

    private Map<String, Object> metadata(Object... keyValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            Object value = keyValues[index + 1];
            if (value != null) {
                values.put(String.valueOf(keyValues[index]), value);
            }
        }
        return values;
    }
}
