package com.ca.centranalytics.integration.ingestion.service;

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
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Абстрактный базовый класс для мапперов входящих событий мессенджеров.
 * <p>
 * Предоставляет общую функциональность для преобразования сообщений
 * из различных платформ (WhatsApp, MAX, Telegram и др.) в единый
 * формат {@link InboundIntegrationEvent}.
 * <p>
 * Наследники должны реализовать платформо-специфичные методы:
 * <ul>
 *   <li>{@code resolveText()} - извлечение текста из сообщения</li>
 *   <li>{@code extractAttachments()} - извлечение вложений</li>
 *   <li>{@code mapMessageType()} - преобразование типа сообщения</li>
 *   <li>{@code buildDisplayName()} - формирование отображаемого имени</li>
 * </ul>
 *
 * @param <T> тип платформо-специфичного DTO сообщения
 */
public abstract class BaseInboundEventMapper<T> {

    protected final ObjectMapper objectMapper;

    protected BaseInboundEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Преобразует платформо-специфичное сообщение в единый формат.
     *
     * @param message    входящее сообщение
     * @param platform   платформа (WhatsApp, MAX, etc.)
     * @param profileId  ID профиля/аккаунта
     * @param profileLabel метка профиля для отображения
     * @return единое событие интеграции
     */
    protected InboundIntegrationEvent mapToEvent(
            T message,
            Platform platform,
            String profileId,
            String profileLabel,
            String eventType,
            String externalEventId,
            String conversationId,
            ConversationType conversationType,
            String conversationTitle,
            Map<String, Object> conversationMetadata,
            String authorId,
            String authorDisplayName,
            String authorPhone,
            Boolean authorIsBot,
            Map<String, Object> authorMetadata,
            String messageId,
            Instant sentAt,
            String text,
            MessageType messageType,
            String replyToId,
            String forwardedFrom,
            List<InboundAttachment> attachments
    ) {
        return new InboundIntegrationEvent(
                platform,
                StringUtils.hasText(eventType) ? eventType : "incoming_message",
                externalEventId,
                writeValue(message),
                true,
                profileId,
                profileLabel,
                writeValue(Map.of("profileId", profileId)),
                new InboundConversation(
                        conversationId,
                        conversationType,
                        conversationTitle,
                        writeValue(conversationMetadata)
                ),
                new InboundAuthor(
                        authorId,
                        authorDisplayName,
                        null,
                        null,
                        null,
                        authorPhone,
                        null,
                        authorIsBot != null && authorIsBot,
                        writeValue(authorMetadata)
                ),
                new InboundMessage(
                        messageId,
                        sentAt,
                        text,
                        text == null ? null : text.toLowerCase(),
                        messageType,
                        replyToId,
                        forwardedFrom,
                        attachments
                )
        );
    }

    /**
     * Создает базовую структуру метаданных из пар ключ-значение.
     */
    protected Map<String, Object> metadata(Object... keyValues) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            metadata.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return metadata;
    }

    /**
     * Преобразует объект в JSON-строку.
     */
    protected String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize inbound message payload", ex);
        }
    }

    /**
     * Парсит timestamp в Instant.
     */
    protected Instant parseTimestamp(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return null;
        }
        return OffsetDateTime.parse(timestamp).toInstant();
    }

    /**
     * Преобразует строковый тип чата в enum.
     */
    protected ConversationType mapConversationType(String chatType) {
        if (!StringUtils.hasText(chatType)) {
            return ConversationType.DIRECT;
        }
        return switch (chatType.toLowerCase()) {
            case "group", "chat" -> ConversationType.GROUP;
            case "channel" -> ConversationType.CHANNEL;
            default -> ConversationType.DIRECT;
        };
    }

    /**
     * Извлекает номер телефона из поля автора.
     */
    protected String extractPhone(String phoneField) {
        return StringUtils.hasText(phoneField) ? phoneField : null;
    }
}
