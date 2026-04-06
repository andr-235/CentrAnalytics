package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.EnrichVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.client.VkFallbackClient;
import com.ca.centranalytics.integration.channel.vk.client.VkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkUserCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VkDiscoveryOrchestrator {

    private final VkOfficialClient vkOfficialClient;
    private final VkFallbackClient vkFallbackClient;
    private final VkOfficialGroupCandidateMapper vkOfficialGroupCandidateMapper;
    private final VkOfficialUserCandidateMapper vkOfficialUserCandidateMapper;
    private final VkWallSnapshotMapper vkWallSnapshotMapper;
    private final VkCommentSnapshotMapper vkCommentSnapshotMapper;
    private final VkCandidatePersistenceService vkCandidatePersistenceService;
    private final VkSnapshotPersistenceService vkSnapshotPersistenceService;
    private final VkFallbackPolicy vkFallbackPolicy;
    private final VkSourceNormalizationService vkSourceNormalizationService;
    private final VkCrawlJobService vkCrawlJobService;
    private final VkPostIngestionMapper vkPostIngestionMapper;
    private final VkCommentIngestionMapper vkCommentIngestionMapper;
    private final VkGroupCandidateRepository vkGroupCandidateRepository;
    private final VkUserCandidateRepository vkUserCandidateRepository;
    private final VkWallPostSnapshotRepository vkWallPostSnapshotRepository;
    private final IntegrationIngestionService integrationIngestionService;

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

    public void runGroupPostCollection(VkCrawlJob job, Long groupId, CollectVkGroupPostsRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));

        List<VkWallPostResult> results = vkOfficialClient.getGroupPosts(groupId, request.limit());
        VkCollectionMethod method = VkCollectionMethod.OFFICIAL_API;
        if (vkFallbackPolicy.shouldFallbackForSearch(request.collectionMode(), results.isEmpty(), false)) {
            List<VkWallPostResult> fallbackResults = vkFallbackClient.getGroupPosts(groupId, request.limit());
            if (!fallbackResults.isEmpty()) {
                results = fallbackResults;
                method = VkCollectionMethod.FALLBACK;
            }
        }

        VkGroupCandidate groupCandidate = ensureGroupCandidate(groupId, method);
        groupCandidate.setSource(vkSourceNormalizationService.resolveGroupSource(groupId, groupCandidate.getName()));
        groupCandidate = vkCandidatePersistenceService.upsertGroupCandidate(groupCandidate);

        Map<Long, VkUserCandidate> authors = enrichUsers(
                extractAuthorIds(results.stream().map(VkWallPostResult::authorVkUserId).toList()),
                request.collectionMode()
        );

        int processed = 0;
        int errors = 0;
        for (VkWallPostResult result : results) {
            try {
                VkWallPostSnapshot snapshot = vkWallSnapshotMapper.map(result, method);
                snapshot.setSource(groupCandidate.getSource());
                VkWallPostSnapshot persisted = vkSnapshotPersistenceService.upsertWallPostSnapshot(snapshot);
                integrationIngestionService.ingest(vkPostIngestionMapper.map(
                        groupCandidate,
                        authors.get(persisted.getAuthorVkUserId()),
                        persisted
                ));
                processed++;
            } catch (RuntimeException ex) {
                errors++;
            }
        }

        finalizeJob(job.getId(), results.size(), processed, errors, results.isEmpty() ? 1 : 0);
    }

    public void runPostCommentCollection(VkCrawlJob job, CollectVkPostCommentsRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));

        List<VkWallPostSnapshot> wallPosts = vkWallPostSnapshotRepository.findAllByPostIdIn(request.postIds());
        if (wallPosts.isEmpty()) {
            finalizeJob(job.getId(), 0, 0, 0, 1);
            return;
        }

        int itemCount = 0;
        int processed = 0;
        int errors = 0;

        for (VkWallPostSnapshot wallPost : wallPosts) {
            List<VkCommentResult> results = vkOfficialClient.getPostComments(wallPost.getOwnerId(), wallPost.getPostId(), request.limit());
            VkCollectionMethod method = VkCollectionMethod.OFFICIAL_API;
            if (vkFallbackPolicy.shouldFallbackForSearch(request.collectionMode(), results.isEmpty(), false)) {
                List<VkCommentResult> fallbackResults = vkFallbackClient.getPostComments(wallPost.getOwnerId(), wallPost.getPostId(), request.limit());
                if (!fallbackResults.isEmpty()) {
                    results = fallbackResults;
                    method = VkCollectionMethod.FALLBACK;
                }
            }

            itemCount += results.size();
            VkGroupCandidate groupCandidate = ensureGroupCandidate(Math.abs(wallPost.getOwnerId()), method);
            groupCandidate.setSource(vkSourceNormalizationService.resolveGroupSource(groupCandidate.getVkGroupId(), groupCandidate.getName()));
            groupCandidate = vkCandidatePersistenceService.upsertGroupCandidate(groupCandidate);

            Map<Long, VkUserCandidate> authors = enrichUsers(
                    extractAuthorIds(results.stream().map(VkCommentResult::authorVkUserId).toList()),
                    request.collectionMode()
            );

            for (VkCommentResult result : results) {
                try {
                    VkCommentSnapshot snapshot = vkCommentSnapshotMapper.map(result, method);
                    snapshot.setSource(groupCandidate.getSource());
                    VkCommentSnapshot persisted = vkSnapshotPersistenceService.upsertCommentSnapshot(snapshot);
                    integrationIngestionService.ingest(vkCommentIngestionMapper.map(
                            groupCandidate,
                            authors.get(persisted.getAuthorVkUserId()),
                            persisted
                    ));
                    processed++;
                } catch (RuntimeException ex) {
                    errors++;
                }
            }
        }

        finalizeJob(job.getId(), itemCount, processed, errors, itemCount == 0 ? 1 : 0);
    }

    public void runUserEnrichment(VkCrawlJob job, EnrichVkUsersRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));
        Map<Long, VkUserCandidate> users = enrichUsers(request.userIds(), request.collectionMode());
        int warnings = users.isEmpty() ? 1 : 0;
        finalizeJob(job.getId(), request.userIds().size(), users.size(), 0, warnings);
    }

    private VkGroupCandidate ensureGroupCandidate(Long groupId, VkCollectionMethod collectionMethod) {
        return vkGroupCandidateRepository.findByVkGroupId(groupId)
                .orElseGet(() -> vkCandidatePersistenceService.upsertGroupCandidate(
                        vkSourceNormalizationService.placeholderGroupCandidate(groupId, null, collectionMethod)
                ));
    }

    private Map<Long, VkUserCandidate> enrichUsers(List<Long> userIds, String collectionMode) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, VkUserSearchResult> resultsById = new LinkedHashMap<>();
        Map<Long, VkCollectionMethod> collectionMethodsById = new LinkedHashMap<>();
        vkOfficialClient.getUsersByIds(userIds)
                .forEach(result -> {
                    resultsById.put(result.id(), result);
                    collectionMethodsById.put(result.id(), VkCollectionMethod.OFFICIAL_API);
                });

        List<Long> missingIds = userIds.stream()
                .filter(id -> !resultsById.containsKey(id))
                .toList();

        if (!missingIds.isEmpty() && vkFallbackPolicy.shouldFallbackForSearch(collectionMode, false, true)) {
            vkFallbackClient.getUsersByIds(missingIds)
                    .forEach(result -> {
                        if (!resultsById.containsKey(result.id())) {
                            resultsById.put(result.id(), result);
                            collectionMethodsById.put(result.id(), VkCollectionMethod.FALLBACK);
                        }
                    });
        }

        Map<Long, VkUserCandidate> candidates = new LinkedHashMap<>();
        resultsById.values().forEach(result -> {
            VkUserCandidate candidate = vkOfficialUserCandidateMapper.map(
                    "",
                    result,
                    collectionMethodsById.getOrDefault(result.id(), VkCollectionMethod.OFFICIAL_API)
            );
            candidates.put(candidate.getVkUserId(), vkCandidatePersistenceService.upsertUserCandidate(candidate));
        });
        return candidates;
    }

    private List<Long> extractAuthorIds(List<Long> authorIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>();
        authorIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .forEach(uniqueIds::add);
        return new ArrayList<>(uniqueIds);
    }

    private void finalizeJob(Long jobId, int itemCount, int processedCount, int errorCount, int warningCount) {
        vkCrawlJobService.update(jobId, current -> {
            current.setItemCount(itemCount);
            current.setProcessedCount(processedCount);
            current.setErrorCount(errorCount);
            current.setWarningCount(warningCount);
            current.setStatus(resolveStatus(processedCount, errorCount));
        });
    }

    private VkCrawlJobStatus resolveStatus(int processedCount, int errorCount) {
        if (errorCount == 0) {
            return VkCrawlJobStatus.COMPLETED;
        }
        return processedCount > 0 ? VkCrawlJobStatus.PARTIAL : VkCrawlJobStatus.FAILED;
    }
}
