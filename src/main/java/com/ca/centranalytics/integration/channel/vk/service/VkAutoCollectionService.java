package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

@Service
public class VkAutoCollectionService {
    private final VkAutoCollectionProperties properties;
    private final VkCrawlCommandService vkCrawlCommandService;
    private final VkGroupCandidateRepository vkGroupCandidateRepository;
    private final VkWallPostSnapshotRepository vkWallPostSnapshotRepository;
    private final LongConsumer delayStrategy;
    private final AtomicBoolean runUserDiscoveryNext = new AtomicBoolean(false);

    @Autowired
    public VkAutoCollectionService(
            VkAutoCollectionProperties properties,
            VkCrawlCommandService vkCrawlCommandService,
            VkGroupCandidateRepository vkGroupCandidateRepository,
            VkWallPostSnapshotRepository vkWallPostSnapshotRepository
    ) {
        this(properties, vkCrawlCommandService, vkGroupCandidateRepository, vkWallPostSnapshotRepository, millis -> {
        });
    }

    public VkAutoCollectionService(
            VkAutoCollectionProperties properties,
            VkCrawlCommandService vkCrawlCommandService,
            VkGroupCandidateRepository vkGroupCandidateRepository,
            VkWallPostSnapshotRepository vkWallPostSnapshotRepository,
            LongConsumer delayStrategy
    ) {
        this.properties = properties;
        this.vkCrawlCommandService = vkCrawlCommandService;
        this.vkGroupCandidateRepository = vkGroupCandidateRepository;
        this.vkWallPostSnapshotRepository = vkWallPostSnapshotRepository;
        this.delayStrategy = delayStrategy;
    }

    public void collect() {
        if (!properties.enabled()) {
            return;
        }

        boolean runUserDiscovery = runUserDiscoveryNext.getAndSet(!runUserDiscoveryNext.get());
        if (runUserDiscovery) {
            vkCrawlCommandService.createUserSearchJob(new SearchVkUsersRequest(
                    properties.region(),
                    properties.groupSearchLimit(),
                    properties.collectionMode()
            ));
        } else {
            vkCrawlCommandService.createGroupSearchJob(new SearchVkGroupsRequest(
                    properties.region(),
                    properties.groupSearchLimit(),
                    properties.collectionMode()
            ));
        }

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
