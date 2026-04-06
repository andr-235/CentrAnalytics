package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.client.VkFallbackClient;
import com.ca.centranalytics.integration.channel.vk.client.VkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VkDiscoveryOrchestrator {

    private final VkOfficialClient vkOfficialClient;
    private final VkFallbackClient vkFallbackClient;
    private final VkOfficialGroupCandidateMapper vkOfficialGroupCandidateMapper;
    private final VkOfficialUserCandidateMapper vkOfficialUserCandidateMapper;
    private final VkCandidatePersistenceService vkCandidatePersistenceService;
    private final VkFallbackPolicy vkFallbackPolicy;
    private final VkCrawlJobService vkCrawlJobService;

    public void runGroupSearch(VkCrawlJob job, SearchVkGroupsRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));

        List<VkGroupSearchResult> results = vkOfficialClient.searchGroups(request.region(), request.limit());
        VkCollectionMethod method = VkCollectionMethod.OFFICIAL_API;
        if (vkFallbackPolicy.shouldFallbackForSearch(request.collectionMode(), results.isEmpty(), false)) {
            List<VkGroupSearchResult> fallbackResults = vkFallbackClient.searchGroups(request.region(), request.limit());
            if (!fallbackResults.isEmpty()) {
                results = fallbackResults;
                method = VkCollectionMethod.FALLBACK;
            }
        }

        VkCollectionMethod finalMethod = method;
        int processed = results.stream()
                .map(result -> vkOfficialGroupCandidateMapper.map(request.region(), result, finalMethod))
                .map(vkCandidatePersistenceService::upsertGroupCandidate)
                .toList()
                .size();

        int warnings = results.isEmpty() ? 1 : 0;
        vkCrawlJobService.update(job.getId(), current -> {
            current.setStatus(VkCrawlJobStatus.COMPLETED);
            current.setItemCount(processed);
            current.setProcessedCount(processed);
            current.setWarningCount(warnings);
        });
    }

    public void runUserSearch(VkCrawlJob job, SearchVkUsersRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));

        List<VkUserSearchResult> results = vkOfficialClient.searchUsers(request.region(), request.limit());
        VkCollectionMethod method = VkCollectionMethod.OFFICIAL_API;
        if (vkFallbackPolicy.shouldFallbackForSearch(request.collectionMode(), results.isEmpty(), false)) {
            List<VkUserSearchResult> fallbackResults = vkFallbackClient.searchUsers(request.region(), request.limit());
            if (!fallbackResults.isEmpty()) {
                results = fallbackResults;
                method = VkCollectionMethod.FALLBACK;
            }
        }

        VkCollectionMethod finalMethod = method;
        int processed = results.stream()
                .map(result -> vkOfficialUserCandidateMapper.map(request.region(), result, finalMethod))
                .map(vkCandidatePersistenceService::upsertUserCandidate)
                .toList()
                .size();

        int warnings = results.isEmpty() ? 1 : 0;
        vkCrawlJobService.update(job.getId(), current -> {
            current.setStatus(VkCrawlJobStatus.COMPLETED);
            current.setItemCount(processed);
            current.setProcessedCount(processed);
            current.setWarningCount(warnings);
        });
    }
}
