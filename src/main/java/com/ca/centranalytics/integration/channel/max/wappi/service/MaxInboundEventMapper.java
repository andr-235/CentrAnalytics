package com.ca.centranalytics.integration.channel.max.wappi.service;

import com.ca.centranalytics.integration.channel.max.wappi.client.dto.MaxContactDto;
import com.ca.centranalytics.integration.channel.max.wappi.client.dto.MaxLocationDto;
import com.ca.centranalytics.integration.channel.max.wappi.client.dto.MaxMessageDto;
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
public class MaxInboundEventMapper {

    private final ObjectMapper objectMapper;

    public InboundIntegrationEvent map(MaxMessageDto message) {
        String text = resolveText(message);
        return new InboundIntegrationEvent(
                Platform.MAX,
                StringUtils.hasText(message.whType()) ? message.whType() : "incoming_message",
                "max-" + message.profileId() + "-" + message.id(),
                writeValue(message),
                true,
                message.profileId(),
                "MAX profile " + message.profileId(),
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
                        extractPhone(message),
                        null,
                        false,
                        writeValue(metadata(
                                "senderName", message.senderName(),
                                "contactName", message.contactName(),
                                "contact", message.contact()
                        ))
                ),
                new InboundMessage(
                        message.id(),
                        parseInstant(message),
                        text,
                        text == null ? null : text.toLowerCase(),
                        mapMessageType(message.type()),
                        null,
                        Boolean.TRUE.equals(message.isForwarded()) ? message.from() : null,
                        extractAttachments(message)
                )
        );
    }

    private List<InboundAttachment> extractAttachments(MaxMessageDto message) {
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
                        "mediaInfo", message.mediaInfo(),
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
            case "text", "location", "contact", "vcard" -> MessageType.TEXT;
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
        if ("thread".equals(chatType)) {
            return ConversationType.THREAD;
        }
        return ConversationType.GROUP;
    }

    private Instant parseInstant(MaxMessageDto message) {
        if (StringUtils.hasText(message.timestamp())) {
            return OffsetDateTime.parse(message.timestamp()).toInstant();
        }
        if (message.time() != null) {
            return Instant.ofEpochSecond(message.time());
        }
        throw new IllegalArgumentException("MAX message timestamp is required");
    }

    private String resolveText(MaxMessageDto message) {
        if ("text".equals(message.type()) && StringUtils.hasText(message.body())) {
            return message.body();
        }
        if ("location".equals(message.type()) && message.location() != null) {
            return formatLocation(message.location());
        }
        if ("contact".equals(message.type()) || "vcard".equals(message.type())) {
            return buildContactText(message.contact());
        }
        if (StringUtils.hasText(message.caption())) {
            return message.caption();
        }
        if (StringUtils.hasText(message.title())) {
            return message.title();
        }
        return null;
    }

    private String resolveAttachmentUrl(MaxMessageDto message) {
        if (StringUtils.hasText(message.fileLink())) {
            return message.fileLink();
        }
        if (message.body() != null && (message.body().startsWith("http://") || message.body().startsWith("https://"))) {
            return message.body();
        }
        return message.picture();
    }

    private String resolveAttachmentContent(MaxMessageDto message) {
        if (StringUtils.hasText(message.fileLink())) {
            return null;
        }
        if (message.body() != null && (message.body().startsWith("http://") || message.body().startsWith("https://"))) {
            return null;
        }
        return message.body();
    }

    private String buildDisplayName(MaxMessageDto message) {
        if (StringUtils.hasText(message.contactName())) {
            return message.contactName();
        }
        if (message.contact() != null && StringUtils.hasText(message.contact().name())) {
            return message.contact().name();
        }
        if (StringUtils.hasText(message.senderName())) {
            return message.senderName();
        }
        return message.from();
    }

    private String buildConversationTitle(MaxMessageDto message) {
        if (StringUtils.hasText(message.contactName())) {
            return message.contactName();
        }
        if (StringUtils.hasText(message.senderName())) {
            return message.senderName();
        }
        return message.chatId();
    }

    private String extractPhone(MaxMessageDto message) {
        MaxContactDto contact = message.contact();
        if (contact != null && StringUtils.hasText(contact.phone())) {
            return contact.phone();
        }
        return null;
    }

    private String formatLocation(MaxLocationDto location) {
        return "Location: " + location.latitude() + "," + location.longitude();
    }

    private String buildContactText(MaxContactDto contact) {
        if (contact == null) {
            return null;
        }
        if (StringUtils.hasText(contact.name()) && StringUtils.hasText(contact.phone())) {
            return contact.name() + " " + contact.phone();
        }
        if (StringUtils.hasText(contact.name())) {
            return contact.name();
        }
        return contact.phone();
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize MAX payload", e);
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
