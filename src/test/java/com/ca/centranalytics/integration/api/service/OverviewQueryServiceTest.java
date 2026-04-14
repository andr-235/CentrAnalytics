package com.ca.centranalytics.integration.api.service;

import com.ca.centranalytics.integration.api.dto.OverviewPlatform;
import com.ca.centranalytics.integration.api.dto.OverviewResponse;
import com.ca.centranalytics.integration.api.dto.OverviewWindow;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSession;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionRepository;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionState;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import com.ca.centranalytics.integration.domain.entity.IntegrationStatus;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import com.ca.centranalytics.integration.domain.repository.OverviewMetricsRepository;
import com.ca.centranalytics.integration.domain.repository.OverviewPlatformMetrics;
import com.ca.centranalytics.integration.domain.repository.OverviewSummaryMetrics;
import com.ca.centranalytics.integration.domain.repository.OverviewTrendBucket;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OverviewQueryServiceTest {

    @Test
    void getOverview_buildsSummaryAndPlatformStatuses() {
        Instant now = Instant.parse("2026-04-10T10:15:00Z");

        PlatformStatusResolver statusResolver = new PlatformStatusResolver();
        OverviewQueryService service = new OverviewQueryService(
                overviewMetricsRepository(),
                integrationSourceRepository(),
                telegramUserSessionRepository(),
                vkGroupCandidateRepository(),
                statusResolver
        );

        OverviewResponse response = service.getOverview(OverviewWindow.HOURS_24, now);

        assertThat(response.summary().messageCount()).isEqualTo(120);
        assertThat(response.summary().platformIssueCount()).isEqualTo(1);
        assertThat(response.platforms())
                .extracting(item -> item.platform())
                .containsExactly(
                        OverviewPlatform.TELEGRAM.apiValue(),
                        OverviewPlatform.VK.apiValue(),
                        OverviewPlatform.WHATSAPP.apiValue(),
                        OverviewPlatform.MAX.apiValue()
                );
        assertThat(response.platforms())
                .filteredOn(item -> item.platform().equals(OverviewPlatform.TELEGRAM.apiValue()))
                .first()
                .satisfies(item -> {
                    assertThat(item.status()).isEqualTo("healthy");
                    assertThat(item.integration().detail()).isEqualTo("Сессия активна");
                    assertThat(item.trend()).hasSize(1);
                });
        assertThat(response.platforms())
                .filteredOn(item -> item.platform().equals(OverviewPlatform.WHATSAPP.apiValue()))
                .first()
                .extracting(item -> item.status())
                .isEqualTo("inactive");
        assertThat(response.platforms())
                .filteredOn(item -> item.platform().equals(OverviewPlatform.MAX.apiValue()))
                .first()
                .satisfies(item -> {
                    assertThat(item.status()).isEqualTo("healthy");
                    assertThat(item.integration().detail()).isEqualTo("Источники MAX: 1");
                    assertThat(item.trend()).hasSize(1);
                });
    }

    private OverviewMetricsRepository overviewMetricsRepository() {
        Map<Platform, List<OverviewTrendBucket>> trends = Map.of(
                Platform.TELEGRAM, List.of(new OverviewTrendBucket(Instant.parse("2026-04-10T09:00:00Z"), 12)),
                Platform.VK, List.of(new OverviewTrendBucket(Instant.parse("2026-04-10T06:00:00Z"), 5)),
                Platform.WAPPI, List.of(),
                Platform.MAX, List.of(new OverviewTrendBucket(Instant.parse("2026-04-10T08:00:00Z"), 4))
        );

        return new OverviewMetricsRepository() {
            @Override
            public OverviewSummaryMetrics fetchSummary(Instant from, Instant to) {
                return new OverviewSummaryMetrics(120, 18, 44);
            }

            @Override
            public List<OverviewPlatformMetrics> fetchPlatformMetrics(Instant from, Instant to) {
                return List.of(
                        new OverviewPlatformMetrics(Platform.TELEGRAM, 90, 12, 30, Instant.parse("2026-04-10T10:00:00Z")),
                        new OverviewPlatformMetrics(Platform.VK, 30, 6, 14, Instant.parse("2026-04-10T02:30:00Z")),
                        new OverviewPlatformMetrics(Platform.WAPPI, 0, 0, 0, null),
                        new OverviewPlatformMetrics(Platform.MAX, 8, 2, 5, Instant.parse("2026-04-10T09:45:00Z"))
                );
            }

            @Override
            public List<OverviewTrendBucket> fetchTrend(Platform platform, Instant from, Instant to, OverviewWindow window) {
                return trends.getOrDefault(platform, List.of());
            }
        };
    }

    private IntegrationSourceRepository integrationSourceRepository() {
        Map<Platform, List<IntegrationSource>> sources = Map.of(
                Platform.TELEGRAM, List.of(source(1L, Platform.TELEGRAM, IntegrationStatus.ACTIVE, "Telegram Source")),
                Platform.VK, List.of(source(2L, Platform.VK, IntegrationStatus.ACTIVE, "VK Source")),
                Platform.WAPPI, List.of(),
                Platform.MAX, List.of(source(3L, Platform.MAX, IntegrationStatus.ACTIVE, "MAX Source"))
        );

        return (IntegrationSourceRepository) Proxy.newProxyInstance(
                IntegrationSourceRepository.class.getClassLoader(),
                new Class[]{IntegrationSourceRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByPlatform" -> sources.getOrDefault((Platform) args[0], List.of());
                    case "countByPlatform" -> (long) sources.getOrDefault((Platform) args[0], List.of()).size();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private TelegramUserSessionRepository telegramUserSessionRepository() {
        TelegramUserSession session = TelegramUserSession.builder()
                .id(7L)
                .phoneNumber("+79991234567")
                .sessionState(TelegramUserSessionState.READY)
                .authorized(true)
                .lastSyncAt(Instant.parse("2026-04-10T10:05:00Z"))
                .updatedAt(Instant.parse("2026-04-10T10:05:00Z"))
                .build();

        return (TelegramUserSessionRepository) Proxy.newProxyInstance(
                TelegramUserSessionRepository.class.getClassLoader(),
                new Class[]{TelegramUserSessionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByOrderByUpdatedAtDesc", "findFirstByAuthorizedTrue" -> Optional.of(session);
                    case "findByAuthorizedTrue" -> List.of(session);
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private VkGroupCandidateRepository vkGroupCandidateRepository() {
        return (VkGroupCandidateRepository) Proxy.newProxyInstance(
                VkGroupCandidateRepository.class.getClassLoader(),
                new Class[]{VkGroupCandidateRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "count" -> 3L;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private IntegrationSource source(Long id, Platform platform, IntegrationStatus status, String name) {
        return IntegrationSource.builder()
                .id(id)
                .platform(platform)
                .name(name)
                .status(status)
                .updatedAt(Instant.parse("2026-04-10T10:00:00Z"))
                .build();
    }
}
