package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VkAutoCollectionService {

    private final VkAutoCollectionProperties properties;
    private final VkCrawlCommandService vkCrawlCommandService;
    private final VkGroupCandidateRepository vkGroupCandidateRepository;
    private final VkWallPostSnapshotRepository vkWallPostSnapshotRepository;

    public void collect() {
        if (!properties.enabled()) {
            return;
        }

        vkCrawlCommandService.createGroupSearchJob(new SearchVkGroupsRequest(
                properties.region(),
                properties.groupSearchLimit(),
                properties.collectionMode()
        ));
        vkCrawlCommandService.createUserSearchJob(new SearchVkUsersRequest(
                properties.region(),
                properties.groupSearchLimit(),
                properties.collectionMode()
        ));

        vkGroupCandidateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(group -> group.getRegionMatchSource() != VkMatchSource.FALLBACK)
                .forEach(group -> {
            Long groupId = group.getVkGroupId();
            vkCrawlCommandService.createGroupPostsJob(groupId, new CollectVkGroupPostsRequest(
                    properties.postLimit(),
                    properties.collectionMode()
            ));

            List<Long> postIds = vkWallPostSnapshotRepository.findAllByOwnerIdOrderByUpdatedAtDesc(-groupId).stream()
                    .limit(properties.commentPostLimit())
                    .map(snapshot -> snapshot.getPostId())
                    .toList();

            if (!postIds.isEmpty()) {
                vkCrawlCommandService.createPostCommentsJob(new CollectVkPostCommentsRequest(
                        postIds,
                        properties.commentLimit(),
                        properties.collectionMode()
                ));
            }
        });
    }
}
