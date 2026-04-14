package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.client.VkFallbackClient;
import com.ca.centranalytics.integration.channel.vk.client.VkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для поиска VK-групп по региону.
 * <p>
 * Отвечает за выполнение задачи поиска групп через official API
 * с автоматическим fallback на парсинг при необходимости.
 */
@Service
@RequiredArgsConstructor
public class VkGroupSearchService {

    private static final int REGIONAL_SEARCH_BATCH_SIZE = 3;

    private final VkOfficialClient vkOfficialClient;
    private final VkFallbackClient vkFallbackClient;
    private final VkOfficialGroupCandidateMapper vkOfficialGroupCandidateMapper;
    private final VkCandidatePersistenceService vkCandidatePersistenceService;
    private final VkFallbackPolicy vkFallbackPolicy;
    private final VkCrawlJobService vkCrawlJobService;

    /**
     * Запускает поиск групп в указанном регионе.
     *
     * @param job     задача VK crawling
     * @param request параметры поиска
     */
    public void runGroupSearch(VkCrawlJob job, SearchVkGroupsRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));
        try {
            List<String> searchTerms = selectRegionalSearchBatch(job, vkOfficialClient.resolveRegionalSearchTerms(request.region()));
            List<GroupSearchHit> results = searchGroups(searchTerms, request.limit(), false);
            VkCollectionMethod method = VkCollectionMethod.OFFICIAL_API;
            if (vkFallbackPolicy.shouldFallbackForSearch(request.collectionMode(), results.isEmpty(), false)) {
                List<GroupSearchHit> fallbackResults = searchGroups(searchTerms, request.limit(), true);
                if (!fallbackResults.isEmpty()) {
                    results = fallbackResults;
                    method = VkCollectionMethod.FALLBACK;
                }
            }

            VkCollectionMethod finalMethod = method;
            Map<Long, VkGroupCandidate> matchedCandidatesById = new LinkedHashMap<>();
            results.stream()
                    .map(hit -> vkOfficialGroupCandidateMapper.map(hit.searchTerm(), hit.result(), finalMethod))
                    .filter(candidate -> candidate.getRegionMatchSource() != VkMatchSource.FALLBACK)
                    .forEach(candidate -> matchedCandidatesById.putIfAbsent(candidate.getVkGroupId(), candidate));
            List<VkGroupCandidate> matchedCandidates = matchedCandidatesById.values().stream()
                    .limit(request.limit())
                    .map(vkCandidatePersistenceService::upsertGroupCandidate)
                    .toList();
            int processed = matchedCandidates.size();

            int warnings = processed == 0 ? 1 : 0;
            vkCrawlJobService.update(job.getId(), current -> {
                current.setStatus(VkCrawlJobStatus.COMPLETED);
                current.setItemCount(processed);
                current.setProcessedCount(processed);
                current.setWarningCount(warnings);
            });
        } catch (RuntimeException ex) {
            finalizeJob(job.getId(), 0, 0, 1);
        }
    }

    private List<GroupSearchHit> searchGroups(List<String> searchTerms, int limit, boolean useFallback) {
        List<GroupSearchHit> results = new ArrayList<>();
        List<String> effectiveTerms = searchTerms.isEmpty() ? List.of() : searchTerms;
        for (String searchTerm : effectiveTerms) {
            List<VkGroupSearchResult> termResults = useFallback
                    ? vkFallbackClient.searchGroups(searchTerm, limit)
                    : vkOfficialClient.searchGroups(searchTerm, limit);
            for (VkGroupSearchResult result : termResults) {
                results.add(new GroupSearchHit(searchTerm, result));
            }
        }
        return List.copyOf(results);
    }

    private List<String> selectRegionalSearchBatch(VkCrawlJob job, List<String> searchTerms) {
        if (searchTerms.size() <= REGIONAL_SEARCH_BATCH_SIZE) {
            return searchTerms;
        }

        int batchCount = (searchTerms.size() + REGIONAL_SEARCH_BATCH_SIZE - 1) / REGIONAL_SEARCH_BATCH_SIZE;
        long batchSeed = job.getId() == null ? 0L : job.getId() - 1L;
        int batchIndex = (int) Math.floorMod(batchSeed, batchCount);
        int fromIndex = batchIndex * REGIONAL_SEARCH_BATCH_SIZE;
        int toIndex = Math.min(fromIndex + REGIONAL_SEARCH_BATCH_SIZE, searchTerms.size());
        return searchTerms.subList(fromIndex, toIndex);
    }

    private void finalizeJob(Long jobId, int itemCount, int processedCount, int errorCount) {
        vkCrawlJobService.update(jobId, current -> {
            current.setItemCount(itemCount);
            current.setProcessedCount(processedCount);
            current.setErrorCount(errorCount);
            current.setStatus(errorCount == 0 ? VkCrawlJobStatus.COMPLETED : VkCrawlJobStatus.FAILED);
        });
    }

    private record GroupSearchHit(String searchTerm, VkGroupSearchResult result) {
    }
}
