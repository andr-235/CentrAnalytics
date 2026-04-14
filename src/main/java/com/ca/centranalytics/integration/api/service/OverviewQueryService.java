package com.ca.centranalytics.integration.api.service;

import com.ca.centranalytics.integration.api.dto.OverviewHighlightResponse;
import com.ca.centranalytics.integration.api.dto.OverviewPlatform;
import com.ca.centranalytics.integration.api.dto.OverviewResponse;
import com.ca.centranalytics.integration.api.dto.OverviewSummaryResponse;
import com.ca.centranalytics.integration.api.dto.OverviewTrendPointResponse;
import com.ca.centranalytics.integration.api.dto.OverviewWindow;
import com.ca.centranalytics.integration.api.dto.PlatformIntegrationStatusResponse;
import com.ca.centranalytics.integration.api.dto.PlatformOverviewResponse;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import com.ca.centranalytics.integration.domain.entity.IntegrationStatus;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import com.ca.centranalytics.integration.domain.repository.OverviewMetricsRepository;
import com.ca.centranalytics.integration.domain.repository.OverviewPlatformMetrics;
import com.ca.centranalytics.integration.domain.repository.OverviewSummaryMetrics;
import com.ca.centranalytics.integration.domain.repository.OverviewTrendBucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OverviewQueryService {

    private static final List<OverviewPlatform> DISPLAY_PLATFORMS = List.of(
            OverviewPlatform.TELEGRAM,
            OverviewPlatform.VK,
            OverviewPlatform.WHATSAPP,
            OverviewPlatform.MAX
    );

    private final OverviewMetricsRepository overviewMetricsRepository;
    private final IntegrationSourceRepository integrationSourceRepository;
    private final TelegramUserSessionRepository telegramUserSessionRepository;
    private final VkGroupCandidateRepository vkGroupCandidateRepository;
    private final PlatformStatusResolver platformStatusResolver;

    public OverviewResponse getOverview(OverviewWindow window, Instant now) {
        Instant from = now.minus(window.duration());
        OverviewSummaryMetrics summaryMetrics = overviewMetricsRepository.fetchSummary(from, now);
        Map<Platform, OverviewPlatformMetrics> metricByPlatform = new EnumMap<>(Platform.class);
        overviewMetricsRepository.fetchPlatformMetrics(from, now)
                .forEach(item -> metricByPlatform.put(item.platform(), item));

        Map<Platform, List<IntegrationSource>> sourcesByPlatform = new EnumMap<>(Platform.class);
        for (OverviewPlatform platform : DISPLAY_PLATFORMS) {
            if (platform.internalPlatform() != null) {
                sourcesByPlatform.put(
                        platform.internalPlatform(),
                        integrationSourceRepository.findByPlatform(platform.internalPlatform())
                );
            }
        }

        TelegramUserSession telegramSession = telegramUserSessionRepository.findFirstByOrderByUpdatedAtDesc()
                .orElse(null);

        List<PlatformOverviewResponse> platforms = DISPLAY_PLATFORMS.stream()
                .map(platform -> buildPlatformOverview(platform, metricByPlatform, sourcesByPlatform, telegramSession, from, now, window, summaryMetrics.messageCount()))
                .toList();

        long platformIssueCount = platforms.stream()
                .filter(item -> !"healthy".equals(item.status()))
                .filter(item -> !"inactive".equals(item.status()))
                .count();

        return new OverviewResponse(
                now,
                window.apiValue(),
                new OverviewSummaryResponse(
                        summaryMetrics.messageCount(),
                        summaryMetrics.conversationCount(),
                        summaryMetrics.activeAuthorCount(),
                        platformIssueCount
                ),
                platforms
        );
    }

    private PlatformOverviewResponse buildPlatformOverview(
            OverviewPlatform platform,
            Map<Platform, OverviewPlatformMetrics> metricByPlatform,
            Map<Platform, List<IntegrationSource>> sourcesByPlatform,
            TelegramUserSession telegramSession,
            Instant from,
            Instant now,
            OverviewWindow window,
            long totalMessageCount
    ) {
        Platform internalPlatform = platform.internalPlatform();
        OverviewPlatformMetrics metrics = internalPlatform == null
                ? null
                : metricByPlatform.get(internalPlatform);
        List<IntegrationSource> sources = internalPlatform == null
                ? List.of()
                : sourcesByPlatform.getOrDefault(internalPlatform, List.of());
        List<OverviewTrendBucket> trend = internalPlatform == null
                ? List.of()
                : overviewMetricsRepository.fetchTrend(internalPlatform, from, now, window);

        PlatformContext context = new PlatformContext(
                metrics == null ? 0L : metrics.messageCount(),
                metrics == null ? 0L : metrics.conversationCount(),
                metrics == null ? 0L : metrics.activeAuthorCount(),
                metrics == null ? null : metrics.lastEventAt(),
                sources.size(),
                sources.stream().anyMatch(source -> source.getStatus() == IntegrationStatus.ERROR),
                sources.stream().allMatch(source -> source.getStatus() == IntegrationStatus.INACTIVE),
                latestSourceUpdate(sources),
                latestSourceError(sources),
                platform == OverviewPlatform.TELEGRAM ? telegramSession : null,
                platform == OverviewPlatform.VK ? vkGroupCandidateRepository.count() : 0L
        );

        String status = platformStatusResolver.resolveStatus(platform, context, now);

        return new PlatformOverviewResponse(
                platform.apiValue(),
                platform.label(),
                status,
                buildHighlights(context, totalMessageCount),
                trend.stream()
                        .map(point -> new OverviewTrendPointResponse(point.timestamp(), point.messageCount()))
                        .toList(),
                buildIntegration(platform, context, status),
                platformStatusResolver.buildAttentionItems(platform, context, status, now)
        );
    }

    private List<OverviewHighlightResponse> buildHighlights(PlatformContext context, long totalMessageCount) {
        List<OverviewHighlightResponse> highlights = new ArrayList<>();
        highlights.add(new OverviewHighlightResponse("Сообщения", String.valueOf(context.messageCount())));
        highlights.add(new OverviewHighlightResponse("Диалоги", String.valueOf(context.conversationCount())));
        highlights.add(new OverviewHighlightResponse("Авторы", String.valueOf(context.activeAuthorCount())));

        String share = totalMessageCount == 0
                ? "0%"
                : Math.round((context.messageCount() * 100.0) / totalMessageCount) + "%";
        highlights.add(new OverviewHighlightResponse("Доля трафика", share));
        return highlights;
    }

    private PlatformIntegrationStatusResponse buildIntegration(
            OverviewPlatform platform,
            PlatformContext context,
            String status
    ) {
        String detail = switch (platform) {
            case TELEGRAM -> telegramDetail(context.telegramSession());
            case VK -> context.vkGroupCount() > 0
                    ? "Группы под управлением: " + context.vkGroupCount()
                    : "Группы не настроены";
            case WHATSAPP -> context.sourceCount() > 0
                    ? "Источники Wappi: " + context.sourceCount()
                    : "Webhook и источники не настроены";
            case MAX -> context.sourceCount() > 0
                    ? "Источники MAX: " + context.sourceCount()
                    : "Webhook и источники не настроены";
        };

        return new PlatformIntegrationStatusResponse(
                status,
                context.lastEventAt(),
                context.lastSuccessAt(),
                context.lastErrorMessage(),
                context.sourceCount(),
                detail
        );
    }

    private Instant latestSourceUpdate(List<IntegrationSource> sources) {
        return sources.stream()
                .map(IntegrationSource::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private String latestSourceError(List<IntegrationSource> sources) {
        return sources.stream()
                .filter(source -> source.getStatus() == IntegrationStatus.ERROR)
                .map(IntegrationSource::getName)
                .findFirst()
                .map(name -> "Ошибка источника: " + name)
                .orElse(null);
    }

    private String telegramDetail(TelegramUserSession session) {
        if (session == null) {
            return "Сессия Telegram не запущена";
        }

        return switch (session.getSessionState()) {
            case READY -> "Сессия активна";
            case WAIT_CODE -> "Ожидается код подтверждения";
            case WAIT_PASSWORD -> "Ожидается пароль";
            case FAILED -> "Требуется восстановление сессии";
            case WAIT_PHONE -> "Ожидается номер телефона";
        };
    }
}
