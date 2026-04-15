package com.ca.centranalytics.integration.api.service;

import java.time.Instant;

/**
 * Контекст платформы с агрегированными метриками.
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
        long vkGroupCount
) {
}
