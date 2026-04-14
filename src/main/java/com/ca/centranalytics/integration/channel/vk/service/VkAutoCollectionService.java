package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobResponse;
import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
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

        try {
            vkCrawlCommandService.createGroupSearchJob(new SearchVkGroupsRequest(
                    properties.region(),
                    properties.groupSearchLimit(),
                    properties.collectionMode()
            ));
        } catch (RuntimeException ex) {
            log.warn("VK auto-collection group search failed for region {}", properties.region(), ex);
            return;
        }

        vkGroupCandidateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(group -> group.getRegionMatchSource() != VkMatchSource.FALLBACK)
                .forEach(group -> {
            Long groupId = group.getVkGroupId();
            VkCrawlJobResponse groupPostsJob;
            try {
                groupPostsJob = vkCrawlCommandService.createGroupPostsJob(groupId, new CollectVkGroupPostsRequest(
                        properties.postLimit(),
                        properties.collectionMode()
                ));
            } catch (RuntimeException ex) {
                log.warn("VK auto-collection group posts failed for group {}", groupId, ex);
                return;
            }

            if (groupPostsJob.status() == VkCrawlJobStatus.FAILED) {
                log.debug("Skipping VK comment collection for group {} because posts job failed", groupId);
                return;
            }

            List<Long> postIds = vkWallPostSnapshotRepository.findAllByOwnerIdOrderByUpdatedAtDesc(-groupId).stream()
                    .limit(properties.commentPostLimit())
                    .map(snapshot -> snapshot.getPostId())
                    .toList();

            if (!postIds.isEmpty()) {
                try {
                    vkCrawlCommandService.createPostCommentsJob(new CollectVkPostCommentsRequest(
                            postIds,
                            properties.commentLimit(),
                            properties.collectionMode()
                    ));
                } catch (RuntimeException ex) {
                    log.warn("VK auto-collection post comments failed for group {}", groupId, ex);
                }
            }
        });
    }
}
