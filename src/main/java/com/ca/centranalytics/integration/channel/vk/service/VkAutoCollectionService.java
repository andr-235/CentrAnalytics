package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VkAutoCollectionService {
    private final VkAutoCollectionProperties properties;
    private final VkCrawlCommandService vkCrawlCommandService;
    private final VkGroupCandidateRepository vkGroupCandidateRepository;
    private final VkWallPostSnapshotRepository vkWallPostSnapshotRepository;

    @Autowired
    public VkAutoCollectionService(
            VkAutoCollectionProperties properties,
            VkCrawlCommandService vkCrawlCommandService,
            VkGroupCandidateRepository vkGroupCandidateRepository,
            VkWallPostSnapshotRepository vkWallPostSnapshotRepository
    ) {
        this.properties = properties;
        this.vkCrawlCommandService = vkCrawlCommandService;
        this.vkGroupCandidateRepository = vkGroupCandidateRepository;
        this.vkWallPostSnapshotRepository = vkWallPostSnapshotRepository;
    }

    public void collect() {
        if (!properties.enabled()) {
            return;
        }

        vkCrawlCommandService.createGroupSearchJob(new SearchVkGroupsRequest(
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
