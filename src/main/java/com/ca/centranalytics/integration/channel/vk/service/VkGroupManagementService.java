package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupCandidateResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupCollectResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupDeleteResponse;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.repository.VkCommentSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VkGroupManagementService {
    private final VkGroupIdentifierResolver vkGroupIdentifierResolver;
    private final VkCrawlCommandService vkCrawlCommandService;
    private final VkWallPostSnapshotRepository vkWallPostSnapshotRepository;
    private final VkCommentSnapshotRepository vkCommentSnapshotRepository;
    private final VkGroupCandidateRepository vkGroupCandidateRepository;
    private final IntegrationSourceRepository integrationSourceRepository;

    public VkGroupCollectResponse collectGroups(
            List<String> groupIdentifiers,
            int postLimit,
            int commentPostLimit,
            int commentLimit
    ) {
        VkResolvedGroupSelection selection = vkGroupIdentifierResolver.resolve(groupIdentifiers);
        List<VkCrawlJobResponse> postJobs = new ArrayList<>();
        List<VkCrawlJobResponse> commentJobs = new ArrayList<>();

        for (VkGroupCandidate group : selection.resolvedGroups()) {
            postJobs.add(vkCrawlCommandService.createGroupPostsJob(
                    group.getVkGroupId(),
                    new CollectVkGroupPostsRequest(postLimit)
            ));

            List<Long> postIds = vkWallPostSnapshotRepository.findAllByOwnerIdOrderByUpdatedAtDesc(-Math.abs(group.getVkGroupId())).stream()
                    .limit(commentPostLimit)
                    .map(snapshot -> snapshot.getPostId())
                    .toList();
            if (!postIds.isEmpty()) {
                commentJobs.add(vkCrawlCommandService.createPostCommentsJob(
                        new CollectVkPostCommentsRequest(postIds, commentLimit)
                ));
            }
        }

        return new VkGroupCollectResponse(
                selection.resolvedGroups().stream().map(this::toResponse).toList(),
                selection.unresolvedIdentifiers(),
                List.copyOf(postJobs),
                List.copyOf(commentJobs)
        );
    }

    @Transactional
    public VkGroupDeleteResponse deleteGroups(List<String> groupIdentifiers) {
        VkResolvedGroupSelection selection = vkGroupIdentifierResolver.resolve(groupIdentifiers);
        List<VkGroupCandidateResponse> deletedGroups = selection.resolvedGroups().stream()
                .map(this::toResponse)
                .toList();

        for (VkGroupCandidate group : selection.resolvedGroups()) {
            Long ownerId = -Math.abs(group.getVkGroupId());
            vkCommentSnapshotRepository.deleteAllByOwnerId(ownerId);
            vkWallPostSnapshotRepository.deleteAllByOwnerId(ownerId);
            vkGroupCandidateRepository.delete(group);
        }

        return new VkGroupDeleteResponse(deletedGroups, selection.unresolvedIdentifiers());
    }

    private VkGroupCandidateResponse toResponse(VkGroupCandidate group) {
        return new VkGroupCandidateResponse(
                group.getId(),
                group.getVkGroupId(),
                group.getSource() != null ? group.getSource().getId() : null,
                group.getScreenName(),
                group.getName(),
                group.getRegionMatchSource(),
                group.getCollectionMethod(),
                group.getRawJson(),
                group.getUpdatedAt()
        );
    }
}
