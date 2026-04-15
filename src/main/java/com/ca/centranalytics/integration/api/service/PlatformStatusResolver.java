package com.ca.centranalytics.integration.api.service;

import com.ca.centranalytics.integration.api.dto.OverviewPlatform;
import com.ca.centranalytics.integration.api.dto.PlatformAttentionItemResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class PlatformStatusResolver {

    private static final Duration INACTIVITY_THRESHOLD = Duration.ofHours(6);

    public String resolveStatus(OverviewPlatform platform, PlatformContext context, Instant now) {
        if (context.sourceCount() == 0 && context.messageCount() == 0) {
            return "inactive";
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

        addVkWarnings(platform, context, items);

        if (context.hasSourceError()) {
            items.add(new PlatformAttentionItemResponse("critical", "Есть источники в состоянии ошибки."));
        }

        if (items.isEmpty()) {
            items.add(new PlatformAttentionItemResponse("healthy", "Критичных сигналов не обнаружено."));
        }

        return items;
    }

    private boolean isInactiveDueToTime(PlatformContext context, Instant now) {
        return context.lastEventAt() != null
                && context.lastEventAt().isBefore(now.minus(INACTIVITY_THRESHOLD));
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
