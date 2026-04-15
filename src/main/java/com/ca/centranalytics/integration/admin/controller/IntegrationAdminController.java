package com.ca.centranalytics.integration.admin.controller;

import com.ca.centranalytics.integration.api.dto.RawEventResponse;
import com.ca.centranalytics.integration.api.service.IntegrationQueryService;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.EnrichVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkCommentSnapshotResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobStatusResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupCandidateResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupCollectRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupCollectResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupDeleteRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupDeleteResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkUserCandidateResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkWallPostSnapshotResponse;
import com.ca.centranalytics.integration.channel.vk.service.VkCrawlCommandService;
import com.ca.centranalytics.integration.channel.vk.service.VkDiscoveryQueryService;
import com.ca.centranalytics.integration.channel.vk.service.VkGroupManagementService;
import com.ca.centranalytics.integration.channel.vk.service.VkJobQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IntegrationAdminController {

    private final IntegrationQueryService integrationQueryService;
    private final VkCrawlCommandService vkCrawlCommandService;
    private final VkJobQueryService vkJobQueryService;
    private final VkDiscoveryQueryService vkDiscoveryQueryService;
    private final VkGroupManagementService vkGroupManagementService;

    @GetMapping("/api/raw-events/{id}")
    public RawEventResponse getRawEvent(@PathVariable Long id) {
        return integrationQueryService.getRawEvent(id);
    }

    @PostMapping("/api/admin/integrations/vk/groups/search")
    public VkCrawlJobResponse searchVkGroups(@Valid @RequestBody SearchVkGroupsRequest request) {
        return vkCrawlCommandService.createGroupSearchJob(request);
    }

    @PostMapping("/api/admin/integrations/vk/users/search")
    public VkCrawlJobResponse searchVkUsers(@Valid @RequestBody SearchVkUsersRequest request) {
        return vkCrawlCommandService.createUserSearchJob(request);
    }

    @PostMapping("/api/admin/integrations/vk/groups/{groupId}/posts/collect")
    public VkCrawlJobResponse collectVkGroupPosts(
            @PathVariable Long groupId,
            @Valid @RequestBody CollectVkGroupPostsRequest request
    ) {
        return vkCrawlCommandService.createGroupPostsJob(groupId, request);
    }

    @PostMapping("/api/admin/integrations/vk/groups/collect")
    public VkGroupCollectResponse collectVkGroups(@Valid @RequestBody VkGroupCollectRequest request) {
        return vkGroupManagementService.collectGroups(
                request.groupIdentifiers(),
                request.resolvedPostLimit(),
                request.resolvedCommentPostLimit(),
                request.resolvedCommentLimit()
        );
    }

    @DeleteMapping("/api/admin/integrations/vk/groups")
    public VkGroupDeleteResponse deleteVkGroups(@Valid @RequestBody VkGroupDeleteRequest request) {
        return vkGroupManagementService.deleteGroups(request.groupIdentifiers());
    }

    @PostMapping("/api/admin/integrations/vk/posts/comments/collect")
    public VkCrawlJobResponse collectVkPostComments(@Valid @RequestBody CollectVkPostCommentsRequest request) {
        return vkCrawlCommandService.createPostCommentsJob(request);
    }

    @PostMapping("/api/admin/integrations/vk/users/enrich")
    public VkCrawlJobResponse enrichVkUsers(@Valid @RequestBody EnrichVkUsersRequest request) {
        return vkCrawlCommandService.createUserEnrichmentJob(request);
    }

    @GetMapping("/api/admin/integrations/vk/jobs/{jobId}")
    public VkCrawlJobStatusResponse getVkJob(@PathVariable Long jobId) {
        return vkJobQueryService.getJob(jobId);
    }

    @GetMapping("/api/admin/integrations/vk/groups")
    public List<VkGroupCandidateResponse> getVkGroups(@RequestParam(required = false) String search) {
        return vkDiscoveryQueryService.getGroups(search);
    }

    @GetMapping("/api/admin/integrations/vk/users")
    public List<VkUserCandidateResponse> getVkUsers(@RequestParam(required = false) String search) {
        return vkDiscoveryQueryService.getUsers(search);
    }

    @GetMapping("/api/admin/integrations/vk/groups/{groupId}/posts")
    public List<VkWallPostSnapshotResponse> getVkGroupPosts(@PathVariable Long groupId) {
        return vkDiscoveryQueryService.getGroupPosts(groupId);
    }

    @GetMapping("/api/admin/integrations/vk/posts/{postId}/comments")
    public List<VkCommentSnapshotResponse> getVkPostComments(
            @PathVariable Long postId,
            @RequestParam Long ownerId
    ) {
        return vkDiscoveryQueryService.getPostComments(ownerId, postId);
    }
}
