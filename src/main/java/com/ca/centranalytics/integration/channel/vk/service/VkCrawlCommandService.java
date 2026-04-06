package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.EnrichVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobResponse;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VkCrawlCommandService {

    private final VkCrawlJobService vkCrawlJobService;
    private final VkDiscoveryOrchestrator vkDiscoveryOrchestrator;

    public VkCrawlJobResponse createGroupSearchJob(SearchVkGroupsRequest request) {
        VkCrawlJob job = vkCrawlJobService.create(VkCrawlJobType.GROUP_SEARCH, request);
        vkDiscoveryOrchestrator.runGroupSearch(job, request);
        return toResponse(vkCrawlJobService.get(job.getId()));
    }

    public VkCrawlJobResponse createUserSearchJob(SearchVkUsersRequest request) {
        VkCrawlJob job = vkCrawlJobService.create(VkCrawlJobType.USER_SEARCH, request);
        vkDiscoveryOrchestrator.runUserSearch(job, request);
        return toResponse(vkCrawlJobService.get(job.getId()));
    }

    public VkCrawlJobResponse createGroupPostsJob(Long groupId, CollectVkGroupPostsRequest request) {
        VkCrawlJob job = vkCrawlJobService.create(
                VkCrawlJobType.GROUP_POSTS,
                Map.of("groupId", groupId, "request", request)
        );
        vkDiscoveryOrchestrator.runGroupPostCollection(job, groupId, request);
        return toResponse(vkCrawlJobService.get(job.getId()));
    }

    public VkCrawlJobResponse createPostCommentsJob(CollectVkPostCommentsRequest request) {
        VkCrawlJob job = vkCrawlJobService.create(VkCrawlJobType.POST_COMMENTS, request);
        vkDiscoveryOrchestrator.runPostCommentCollection(job, request);
        return toResponse(vkCrawlJobService.get(job.getId()));
    }

    public VkCrawlJobResponse createUserEnrichmentJob(EnrichVkUsersRequest request) {
        VkCrawlJob job = vkCrawlJobService.create(VkCrawlJobType.AUTHOR_PROFILE_ENRICH, request);
        vkDiscoveryOrchestrator.runUserEnrichment(job, request);
        return toResponse(vkCrawlJobService.get(job.getId()));
    }

    private VkCrawlJobResponse toResponse(VkCrawlJob job) {
        return new VkCrawlJobResponse(job.getId(), job.getJobType(), job.getStatus());
    }
}
