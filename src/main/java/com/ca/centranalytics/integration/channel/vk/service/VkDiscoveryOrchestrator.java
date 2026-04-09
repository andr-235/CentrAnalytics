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
import com.ca.centranalytics.integration.channel.vk.client.dto.VkRegionalCity;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
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
    private static final long USER_SEARCH_THROTTLE_MS = 1_000L;
    private static final int REGIONAL_SEARCH_BATCH_SIZE = 3;

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
            finalizeJob(job.getId(), 0, 0, 1, 0);
        }
    }

    public void runUserSearch(VkCrawlJob job, SearchVkUsersRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));
        try {
            List<VkRegionalCity> regionalCities = vkOfficialClient.resolveRegionalCities(request.region());
            List<VkRegionalCity> cityBatch = selectUserSearchCityBatch(job, regionalCities);
            Map<Long, VkUserCandidate> matchedCandidatesById = collectUserCandidates(cityBatch, request.limit(), false);
            VkCollectionMethod method = VkCollectionMethod.OFFICIAL_API;
            if (vkFallbackPolicy.shouldFallbackForSearch(request.collectionMode(), matchedCandidatesById.isEmpty(), false)) {
                Map<Long, VkUserCandidate> fallbackCandidates = collectFallbackUserCandidates(cityBatch, request.limit());
                if (!fallbackCandidates.isEmpty()) {
                    matchedCandidatesById = fallbackCandidates;
                    method = VkCollectionMethod.FALLBACK;
                }
            }

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
            finalizeJob(job.getId(), 0, 0, 1, 0);
        }
    }

    public void runGroupPostCollection(VkCrawlJob job, Long groupId, CollectVkGroupPostsRequest request) {
        vkCrawlJobService.update(job.getId(), current -> current.setStatus(VkCrawlJobStatus.RUNNING));

        List<VkWallPostResult> results;
        try {
            results = vkOfficialClient.getGroupPosts(resolveGroupDomain(groupId), request.limit());
        } catch (RuntimeException ex) {
            finalizeJob(job.getId(), 0, 0, 1, 0);
            return;
        }
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
            List<VkCommentResult> results;
            try {
                results = vkOfficialClient.getPostComments(wallPost.getOwnerId(), wallPost.getPostId(), request.limit());
            } catch (RuntimeException ex) {
                errors++;
                continue;
            }
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

    private String resolveGroupDomain(Long groupId) {
        return vkGroupCandidateRepository.findByVkGroupId(groupId)
                .map(VkGroupCandidate::getScreenName)
                .filter(Objects::nonNull)
                .filter(screenName -> !screenName.isBlank())
                .orElse("club" + groupId);
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

    private List<UserSearchHit> searchUsers(List<String> searchTerms, int limit, boolean useFallback) {
        List<UserSearchHit> results = new ArrayList<>();
        List<String> effectiveTerms = searchTerms.isEmpty() ? List.of() : searchTerms;
        for (int index = 0; index < effectiveTerms.size(); index++) {
            if (!useFallback && index > 0) {
                throttleUserSearch();
            }
            String searchTerm = effectiveTerms.get(index);
            List<VkUserSearchResult> termResults = useFallback
                    ? vkFallbackClient.searchUsers(searchTerm, limit)
                    : vkOfficialClient.searchUsers(searchTerm, limit);
            for (VkUserSearchResult result : termResults) {
                results.add(new UserSearchHit(searchTerm, result));
            }
        }
        return List.copyOf(results);
    }

    private Map<Long, VkUserCandidate> collectUserCandidates(List<VkRegionalCity> regionalCities, int limit, boolean useFallback) {
        Map<Long, VkUserCandidate> matchedCandidatesById = new LinkedHashMap<>();
        List<VkRegionalCity> effectiveCities = regionalCities.isEmpty() ? List.of() : regionalCities;
        for (int index = 0; index < effectiveCities.size() && matchedCandidatesById.size() < limit; index++) {
            if (!useFallback && index > 0) {
                throttleUserSearch();
            }
            VkRegionalCity regionalCity = effectiveCities.get(index);
            List<VkUserSearchResult> results = useFallback
                    ? vkFallbackClient.searchUsers(regionalCity.title(), limit)
                    : vkOfficialClient.searchUsers(regionalCity, limit);
            for (VkUserSearchResult result : results) {
                VkUserCandidate candidate = vkOfficialUserCandidateMapper.map(
                        regionalCity.title(),
                        result,
                        useFallback ? VkCollectionMethod.FALLBACK : VkCollectionMethod.OFFICIAL_API
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

    private Map<Long, VkUserCandidate> collectFallbackUserCandidates(List<VkRegionalCity> regionalCities, int limit) {
        return collectUserCandidates(regionalCities, limit, true);
    }

    private List<String> selectRegionalSearchBatch(VkCrawlJob job, List<String> searchTerms) {
        return selectBatch(job, searchTerms);
    }

    private List<VkRegionalCity> selectUserSearchCityBatch(VkCrawlJob job, List<VkRegionalCity> regionalCities) {
        return selectBatch(job, regionalCities);
    }

    private <T> List<T> selectBatch(VkCrawlJob job, List<T> items) {
        if (items.size() <= REGIONAL_SEARCH_BATCH_SIZE) {
            return items;
        }

        int batchCount = (items.size() + REGIONAL_SEARCH_BATCH_SIZE - 1) / REGIONAL_SEARCH_BATCH_SIZE;
        long batchSeed = job.getId() == null ? 0L : job.getId() - 1L;
        int batchIndex = (int) Math.floorMod(batchSeed, batchCount);
        int fromIndex = batchIndex * REGIONAL_SEARCH_BATCH_SIZE;
        int toIndex = Math.min(fromIndex + REGIONAL_SEARCH_BATCH_SIZE, items.size());
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

    private record GroupSearchHit(String searchTerm, VkGroupSearchResult result) {
    }

    private record UserSearchHit(String searchTerm, VkUserSearchResult result) {
    }
}
