package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobResponse;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.service.VkAutoCollectionService;
import com.ca.centranalytics.integration.channel.vk.service.VkCrawlCommandService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VkAutoCollectionServiceTest {

    @Test
    void runsSearchThenCollectsPostsAndCommentsForRegionalGroups() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(true, "Primorsky Krai", 25, 10, 5, 20, "HYBRID", 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(VkGroupCandidate.builder().vkGroupId(1001L).build())),
                wallPostRepository(List.of(
                        VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build(),
                        VkWallPostSnapshot.builder().ownerId(-1001L).postId(3004L).build()
                ))
        );

        service.collect();

        assertThat(vkCrawlCommandService.searchRequests)
                .containsExactly(new SearchVkGroupsRequest("Primorsky Krai", 25, "HYBRID"));
        assertThat(vkCrawlCommandService.userSearchRequests)
                .containsExactly(new SearchVkUsersRequest("Primorsky Krai", 25, "HYBRID"));
        assertThat(vkCrawlCommandService.postRequests)
                .containsExactly(new GroupPostCall(1001L, new CollectVkGroupPostsRequest(10, "HYBRID")));
        assertThat(vkCrawlCommandService.commentRequests)
                .containsExactly(new CollectVkPostCommentsRequest(List.of(3003L, 3004L), 20, "HYBRID"));
    }

    @Test
    void skipsFallbackGroupsDuringAutoCollection() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(true, "Еврейская автономная область", 25, 10, 5, 20, "HYBRID", 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(
                        VkGroupCandidate.builder().vkGroupId(1001L).regionMatchSource(VkMatchSource.TEXT).build(),
                        VkGroupCandidate.builder().vkGroupId(2002L).regionMatchSource(VkMatchSource.FALLBACK).build()
                )),
                wallPostRepository(List.of(VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build()))
        );

        service.collect();

        assertThat(vkCrawlCommandService.postRequests)
                .containsExactly(new GroupPostCall(1001L, new CollectVkGroupPostsRequest(10, "HYBRID")));
        assertThat(vkCrawlCommandService.commentRequests)
                .containsExactly(new CollectVkPostCommentsRequest(List.of(3003L), 20, "HYBRID"));
    }

    @Test
    void doesNothingWhenAutoCollectionIsDisabled() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(false, "Primorsky Krai", 25, 10, 5, 20, "HYBRID", 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(VkGroupCandidate.builder().vkGroupId(1001L).build())),
                wallPostRepository(List.of(VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build()))
        );

        service.collect();

        assertThat(vkCrawlCommandService.searchRequests).isEmpty();
        assertThat(vkCrawlCommandService.userSearchRequests).isEmpty();
        assertThat(vkCrawlCommandService.postRequests).isEmpty();
        assertThat(vkCrawlCommandService.commentRequests).isEmpty();
    }

    private static VkGroupCandidateRepository groupRepository(List<VkGroupCandidate> groups) {
        return (VkGroupCandidateRepository) Proxy.newProxyInstance(
                VkGroupCandidateRepository.class.getClassLoader(),
                new Class[]{VkGroupCandidateRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByOrderByUpdatedAtDesc".equals(method.getName())) {
                        return groups;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static VkWallPostSnapshotRepository wallPostRepository(List<VkWallPostSnapshot> posts) {
        return (VkWallPostSnapshotRepository) Proxy.newProxyInstance(
                VkWallPostSnapshotRepository.class.getClassLoader(),
                new Class[]{VkWallPostSnapshotRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByOwnerIdOrderByUpdatedAtDesc".equals(method.getName())) {
                        return posts;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static final class RecordingVkCrawlCommandService extends VkCrawlCommandService {
        private final List<SearchVkGroupsRequest> searchRequests = new ArrayList<>();
        private final List<SearchVkUsersRequest> userSearchRequests = new ArrayList<>();
        private final List<GroupPostCall> postRequests = new ArrayList<>();
        private final List<CollectVkPostCommentsRequest> commentRequests = new ArrayList<>();

        private RecordingVkCrawlCommandService() {
            super(null, null);
        }

        @Override
        public VkCrawlJobResponse createGroupSearchJob(SearchVkGroupsRequest request) {
            searchRequests.add(request);
            return new VkCrawlJobResponse(1L, VkCrawlJobType.GROUP_SEARCH, VkCrawlJobStatus.COMPLETED);
        }

        @Override
        public VkCrawlJobResponse createUserSearchJob(SearchVkUsersRequest request) {
            userSearchRequests.add(request);
            return new VkCrawlJobResponse(4L, VkCrawlJobType.USER_SEARCH, VkCrawlJobStatus.COMPLETED);
        }

        @Override
        public VkCrawlJobResponse createGroupPostsJob(Long groupId, CollectVkGroupPostsRequest request) {
            postRequests.add(new GroupPostCall(groupId, request));
            return new VkCrawlJobResponse(2L, VkCrawlJobType.GROUP_POSTS, VkCrawlJobStatus.COMPLETED);
        }

        @Override
        public VkCrawlJobResponse createPostCommentsJob(CollectVkPostCommentsRequest request) {
            commentRequests.add(request);
            return new VkCrawlJobResponse(3L, VkCrawlJobType.POST_COMMENTS, VkCrawlJobStatus.COMPLETED);
        }
    }

    private record GroupPostCall(Long groupId, CollectVkGroupPostsRequest request) {
    }
}
