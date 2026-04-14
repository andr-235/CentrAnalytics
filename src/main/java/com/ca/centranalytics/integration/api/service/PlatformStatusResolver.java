package com.ca.centranalytics.integration.api.service;

import com.ca.centranalytics.integration.api.dto.OverviewPlatform;
import com.ca.centranalytics.integration.api.dto.PlatformAttentionItemResponse;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionState;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Определяет статус платформы и формирует список элементов требующих внимания.
 * <p>
 * Анализирует состояние платформы на основе:
 * <ul>
 *   <li>Активности источников</li>
 *   <li>Времени последнего события</li>
 *   <li>Состояния сессий (для Telegram)</li>
 *   <li>Наличия ошибок в источниках</li>
 * </ul>
 */
@org.springframework.stereotype.Component
public class PlatformStatusResolver {

    private static final Duration INACTIVITY_THRESHOLD = Duration.ofHours(6);

    /**
     * Определяет общий статус платформы.
     *
     * @param platform платформа
     * @param context  контекст с метриками
     * @param now      текущее время
     * @return статус: "inactive", "critical", "warning", или "healthy"
     */
    public String resolveStatus(OverviewPlatform platform, PlatformContext context, Instant now) {
        if (context.sourceCount() == 0 && context.messageCount() == 0) {
            return "inactive";
        }

        if (platform == OverviewPlatform.TELEGRAM && context.telegramSession() != null) {
            String sessionStatus = resolveSessionStatus(context.telegramSession());
            if (sessionStatus != null) {
                return sessionStatus;
            }
        }

        if (context.hasSourceError()) {
            return "critical";
        }

        if (isInactiveDueToTime(context, now)) {
            return "warning";
        }

        if (context.allSourcesInactive() && context.messageCount() == 0) {
            return "inactive";
        }

        return "healthy";
    }

    /**
     * Формирует список элементов требующих внимания.
     *
     * @param platform платформа
     * @param context  контекст с метриками
     * @param status   текущий статус платформы
     * @param now      текущее время
     * @return список проблем/рекомендаций
     */
    public List<PlatformAttentionItemResponse> buildAttentionItems(
            OverviewPlatform platform,
            PlatformContext context,
            String status,
            Instant now
    ) {
        List<PlatformAttentionItemResponse> items = new ArrayList<>();

        if ("inactive".equals(status)) {
            items.add(new PlatformAttentionItemResponse("idle", "Платформа пока не настроена или неактивна."));
            return items;
        }

        if (isInactiveDueToTime(context, now)) {
            items.add(new PlatformAttentionItemResponse("warning", "Нет новых событий более 6 часов."));
        }

        addTelegramWarnings(platform, context, items);
        addVkWarnings(platform, context, items);

        if (context.hasSourceError()) {
            items.add(new PlatformAttentionItemResponse("critical", "Есть источники в состоянии ошибки."));
        }

        if (items.isEmpty()) {
            items.add(new PlatformAttentionItemResponse("healthy", "Критичных сигналов не обнаружено."));
        }

        return items;
    }

    private String resolveSessionStatus(TelegramUserSession session) {
        TelegramUserSessionState state = session.getSessionState();
        return switch (state) {
            case FAILED -> "critical";
            case WAIT_PASSWORD, WAIT_CODE -> "warning";
            default -> null;
        };
    }

    private boolean isInactiveDueToTime(PlatformContext context, Instant now) {
        return context.lastEventAt() != null
                && context.lastEventAt().isBefore(now.minus(INACTIVITY_THRESHOLD));
    }

    private void addTelegramWarnings(
            OverviewPlatform platform,
            PlatformContext context,
            List<PlatformAttentionItemResponse> items
    ) {
        if (platform != OverviewPlatform.TELEGRAM || context.telegramSession() == null) {
            return;
        }

        TelegramUserSessionState state = context.telegramSession().getSessionState();
        if (state == TelegramUserSessionState.WAIT_PASSWORD) {
            items.add(new PlatformAttentionItemResponse("warning", "Сессия Telegram ожидает пароль."));
        }
        if (state == TelegramUserSessionState.FAILED) {
            items.add(new PlatformAttentionItemResponse("critical", "Сессия Telegram завершилась ошибкой."));
        }
    }

    private void addVkWarnings(
            OverviewPlatform platform,
            PlatformContext context,
            List<PlatformAttentionItemResponse> items
    ) {
        if (platform == OverviewPlatform.VK && context.vkGroupCount() == 0) {
            items.add(new PlatformAttentionItemResponse("warning", "Нет подключённых VK-групп для сбора."));
        }
    }
}
