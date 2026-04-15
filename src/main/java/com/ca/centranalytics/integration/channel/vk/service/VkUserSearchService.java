package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.client.VkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkRegionalCity;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VkUserSearchService {

    private static final long USER_SEARCH_THROTTLE_MS = 1_000L;

    private final VkOfficialClient vkOfficialClient;
    private final VkOfficialUserCandidateMapper vkOfficialUserCandidateMapper;
    private final VkCandidatePersistenceService vkCandidatePersistenceService;
    private final VkCrawlJobService vkCrawlJobService;

    public void runUserSearch(VkCrawlJob job, SearchVkUsersRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));
        try {
            List<VkRegionalCity> regionalCities = vkOfficialClient.resolveRegionalCities(request.region());
            List<VkRegionalCity> cityBatch = selectUserSearchCityBatch(job, regionalCities);
            Map<Long, VkUserCandidate> matchedCandidatesById = collectUserCandidates(cityBatch, request.limit());

            List<VkUserCandidate> matchedCandidates = matchedCandidatesById.values().stream()
                    .limit(request.limit())
                    .map(vkCandidatePersistenceService::upsertUserCandidate)
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

    private Map<Long, VkUserCandidate> collectUserCandidates(List<VkRegionalCity> regionalCities, int limit) {
        Map<Long, VkUserCandidate> matchedCandidatesById = new LinkedHashMap<>();
        List<VkRegionalCity> effectiveCities = regionalCities.isEmpty() ? List.of() : regionalCities;
        for (int index = 0; index < effectiveCities.size() && matchedCandidatesById.size() < limit; index++) {
            if (index > 0) {
                throttleUserSearch();
            }
            VkRegionalCity regionalCity = effectiveCities.get(index);
            List<VkUserSearchResult> results = vkOfficialClient.searchUsers(regionalCity, limit);
            for (VkUserSearchResult result : results) {
                VkUserCandidate candidate = vkOfficialUserCandidateMapper.map(
                        regionalCity.title(),
                        result,
                        VkCollectionMethod.OFFICIAL_API
                );
                if (candidate.getRegionMatchSource() != VkMatchSource.FALLBACK) {
                    matchedCandidatesById.putIfAbsent(candidate.getVkUserId(), candidate);
                    if (matchedCandidatesById.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return matchedCandidatesById;
    }

    private List<VkRegionalCity> selectUserSearchCityBatch(VkCrawlJob job, List<VkRegionalCity> regionalCities) {
        return selectBatch(job, regionalCities);
    }

    private <T> List<T> selectBatch(VkCrawlJob job, List<T> items) {
        int batchSize = 3;
        if (items.size() <= batchSize) {
            return items;
        }

        int batchCount = (items.size() + batchSize - 1) / batchSize;
        long batchSeed = job.getId() == null ? 0L : job.getId() - 1L;
        int batchIndex = (int) Math.floorMod(batchSeed, batchCount);
        int fromIndex = batchIndex * batchSize;
        int toIndex = Math.min(fromIndex + batchSize, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private void throttleUserSearch() {
        try {
            Thread.sleep(USER_SEARCH_THROTTLE_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("VK user search interrupted", ex);
        }
    }

    private void finalizeJob(Long jobId, int itemCount, int processedCount, int errorCount) {
        vkCrawlJobService.update(jobId, current -> {
            current.setItemCount(itemCount);
            current.setProcessedCount(processedCount);
            current.setErrorCount(errorCount);
            current.setStatus(errorCount == 0 ? VkCrawlJobStatus.COMPLETED : VkCrawlJobStatus.FAILED);
        });
    }
}
