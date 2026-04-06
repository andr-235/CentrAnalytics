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

    public VkCrawlJobResponse createGroupSearchJob(SearchVkGroupsRequest request) {
        return toResponse(vkCrawlJobService.create(VkCrawlJobType.GROUP_SEARCH, request));
    }

    public VkCrawlJobResponse createUserSearchJob(SearchVkUsersRequest request) {
        return toResponse(vkCrawlJobService.create(VkCrawlJobType.USER_SEARCH, request));
    }

    public VkCrawlJobResponse createGroupPostsJob(Long groupId, CollectVkGroupPostsRequest request) {
        return toResponse(vkCrawlJobService.create(
                VkCrawlJobType.GROUP_POSTS,
                Map.of("groupId", groupId, "request", request)
        ));
    }

    public VkCrawlJobResponse createPostCommentsJob(CollectVkPostCommentsRequest request) {
        return toResponse(vkCrawlJobService.create(VkCrawlJobType.POST_COMMENTS, request));
    }

    public VkCrawlJobResponse createUserEnrichmentJob(EnrichVkUsersRequest request) {
        return toResponse(vkCrawlJobService.create(VkCrawlJobType.AUTHOR_PROFILE_ENRICH, request));
    }

    private VkCrawlJobResponse toResponse(VkCrawlJob job) {
        return new VkCrawlJobResponse(job.getId(), job.getJobType(), job.getStatus());
    }
}
