package com.ca.centranalytics.integration.api.service;

import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;

import java.time.Instant;

/**
 * Контекст платформы с агрегированными метриками.
 * <p>
 * Содержит все необходимые данные для определения статуса платформы
 * и формирования элементов требующих внимания.
 *
 * @param messageCount       количество сообщений
 * @param conversationCount  количество диалогов
 * @param activeAuthorCount  количество активных авторов
 * @param lastEventAt        время последнего события
 * @param sourceCount        количество источников
 * @param hasSourceError     есть ли ошибки в источниках
 * @param allSourcesInactive все ли источники неактивны
 * @param lastSuccessAt      время последнего успешного обновления
 * @param lastErrorMessage   последнее сообщение об ошибке
 * @param telegramSession    сессия Telegram (если есть)
 * @param vkGroupCount       количество VK-групп
 */
public record PlatformContext(
        long messageCount,
        long conversationCount,
        long activeAuthorCount,
        Instant lastEventAt,
        long sourceCount,
        boolean hasSourceError,
        boolean allSourcesInactive,
        Instant lastSuccessAt,
        String lastErrorMessage,
        TelegramUserSession telegramSession,
        long vkGroupCount
) {
}
