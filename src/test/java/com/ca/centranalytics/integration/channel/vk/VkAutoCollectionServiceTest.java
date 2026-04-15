package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobResponse;
import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.service.VkAutoCollectionService;
import com.ca.centranalytics.integration.channel.vk.service.VkCrawlCommandService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VkAutoCollectionServiceTest {

    @Test
    void runsGroupDiscoveryWhileCollectingPostsAndCommentsForRegionalGroups() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(true, "Primorsky Krai", 25, 10, 5, 20, 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(VkGroupCandidate.builder().vkGroupId(1001L).regionMatchSource(VkMatchSource.TEXT).build())),
                wallPostRepository(List.of(
                        VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build(),
                        VkWallPostSnapshot.builder().ownerId(-1001L).postId(3004L).build()
                ))
        );

        service.collect();

        assertThat(vkCrawlCommandService.searchRequests)
                .containsExactly(new SearchVkGroupsRequest("Primorsky Krai", 25));
        assertThat(vkCrawlCommandService.userSearchRequests).isEmpty();
        assertThat(vkCrawlCommandService.postRequests)
                .containsExactly(new GroupPostCall(1001L, new CollectVkGroupPostsRequest(10)));
        assertThat(vkCrawlCommandService.commentRequests)
                .containsExactly(new CollectVkPostCommentsRequest(List.of(3003L, 3004L), 20));
    }

    @Test
    void skipsFallbackGroupsDuringAutoCollection() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(true, "Еврейская автономная область", 25, 10, 5, 20, 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(
                        VkGroupCandidate.builder().vkGroupId(1001L).regionMatchSource(VkMatchSource.TEXT).build(),
                        VkGroupCandidate.builder().vkGroupId(2002L).regionMatchSource(VkMatchSource.FALLBACK).build()
                )),
                wallPostRepository(List.of(VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build()))
        );

        service.collect();

        assertThat(vkCrawlCommandService.postRequests)
                .containsExactly(new GroupPostCall(1001L, new CollectVkGroupPostsRequest(10)));
        assertThat(vkCrawlCommandService.commentRequests)
                .containsExactly(new CollectVkPostCommentsRequest(List.of(3003L), 20));
    }

    @Test
    void skipsCommentCollectionWhenGroupPostsJobFails() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        vkCrawlCommandService.failedGroupPostIds.add(1001L);
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(true, "Primorsky Krai", 25, 10, 5, 20, 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(VkGroupCandidate.builder().vkGroupId(1001L).regionMatchSource(VkMatchSource.TEXT).build())),
                wallPostRepository(List.of(VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build()))
        );

        service.collect();

        assertThat(vkCrawlCommandService.postRequests)
                .containsExactly(new GroupPostCall(1001L, new CollectVkGroupPostsRequest(10)));
        assertThat(vkCrawlCommandService.commentRequests).isEmpty();
    }

    @Test
    void continuesCollectingWhenSingleGroupFails() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        vkCrawlCommandService.groupPostExceptions.add(1001L);
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(true, "Primorsky Krai", 25, 10, 5, 20, 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(
                        VkGroupCandidate.builder().vkGroupId(1001L).regionMatchSource(VkMatchSource.TEXT).build(),
                        VkGroupCandidate.builder().vkGroupId(2002L).regionMatchSource(VkMatchSource.TEXT).build()
                )),
                wallPostRepository(List.of(
                        VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build(),
                        VkWallPostSnapshot.builder().ownerId(-2002L).postId(4004L).build()
                ))
        );

        service.collect();

        assertThat(vkCrawlCommandService.postRequests)
                .containsExactly(
                        new GroupPostCall(1001L, new CollectVkGroupPostsRequest(10)),
                        new GroupPostCall(2002L, new CollectVkGroupPostsRequest(10))
                );
        assertThat(vkCrawlCommandService.commentRequests)
                .containsExactly(new CollectVkPostCommentsRequest(List.of(4004L), 20));
    }

    @Test
    void skipsGroupsWithActivePostCollectionBlock() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(true, "Primorsky Krai", 25, 10, 5, 20, 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(
                        VkGroupCandidate.builder().vkGroupId(1001L).regionMatchSource(VkMatchSource.TEXT).postCollectionBlockedUntil(Instant.now().plusSeconds(3600)).build(),
                        VkGroupCandidate.builder().vkGroupId(2002L).regionMatchSource(VkMatchSource.TEXT).build()
                )),
                wallPostRepository(List.of(
                        VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build(),
                        VkWallPostSnapshot.builder().ownerId(-2002L).postId(4004L).build()
                ))
        );

        service.collect();

        assertThat(vkCrawlCommandService.postRequests)
                .containsExactly(new GroupPostCall(2002L, new CollectVkGroupPostsRequest(10)));
        assertThat(vkCrawlCommandService.commentRequests)
                .containsExactly(new CollectVkPostCommentsRequest(List.of(4004L), 20));
    }

    @Test
    void blocksGroupWhenPostsJobThrows() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        vkCrawlCommandService.groupPostExceptions.add(1001L);
        VkGroupCandidateRepository groups = groupRepository(List.of(
                VkGroupCandidate.builder().vkGroupId(1001L).regionMatchSource(VkMatchSource.TEXT).build()
        ));
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(true, "Primorsky Krai", 25, 10, 5, 20, 900000L),
                vkCrawlCommandService,
                groups,
                wallPostRepository(List.of(VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build()))
        );

        service.collect();

        assertThat(groups.findByVkGroupId(1001L))
                .get()
                .extracting(VkGroupCandidate::getPostCollectionBlockedUntil)
                .isNotNull();
    }

    @Test
    void doesNothingWhenAutoCollectionIsDisabled() {
        RecordingVkCrawlCommandService vkCrawlCommandService = new RecordingVkCrawlCommandService();
        VkAutoCollectionService service = new VkAutoCollectionService(
                new VkAutoCollectionProperties(false, "Primorsky Krai", 25, 10, 5, 20, 900000L),
                vkCrawlCommandService,
                groupRepository(List.of(VkGroupCandidate.builder().vkGroupId(1001L).regionMatchSource(VkMatchSource.TEXT).build())),
                wallPostRepository(List.of(VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build()))
        );

        service.collect();

        assertThat(vkCrawlCommandService.searchRequests).isEmpty();
        assertThat(vkCrawlCommandService.userSearchRequests).isEmpty();
        assertThat(vkCrawlCommandService.postRequests).isEmpty();
        assertThat(vkCrawlCommandService.commentRequests).isEmpty();
    }

    private static VkGroupCandidateRepository groupRepository(List<VkGroupCandidate> groups) {
        List<VkGroupCandidate> state = new ArrayList<>(groups);
        return (VkGroupCandidateRepository) Proxy.newProxyInstance(
                VkGroupCandidateRepository.class.getClassLoader(),
                new Class[]{VkGroupCandidateRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByOrderByUpdatedAtDesc" -> new ArrayList<>(state);
                    case "findByVkGroupId" -> state.stream().filter(group -> args[0].equals(group.getVkGroupId())).findFirst();
                    case "save" -> {
                        VkGroupCandidate entity = (VkGroupCandidate) args[0];
                        state.removeIf(group -> entity.getVkGroupId().equals(group.getVkGroupId()));
                        state.add(entity);
                        yield entity;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static VkWallPostSnapshotRepository wallPostRepository(List<VkWallPostSnapshot> posts) {
        return (VkWallPostSnapshotRepository) Proxy.newProxyInstance(
                VkWallPostSnapshotRepository.class.getClassLoader(),
                new Class[]{VkWallPostSnapshotRepository.class},
                (proxy, method, args) -> {
                    if ("findAllByOwnerIdOrderByUpdatedAtDesc".equals(method.getName())) {
                        Long ownerId = (Long) args[0];
                        return posts.stream()
                                .filter(post -> ownerId.equals(post.getOwnerId()))
                                .toList();
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
        private final Set<Long> failedGroupPostIds = new HashSet<>();
        private final Set<Long> groupPostExceptions = new HashSet<>();

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
            if (groupPostExceptions.contains(groupId)) {
                throw new IllegalStateException("VK Access denied");
            }
            if (failedGroupPostIds.contains(groupId)) {
                return new VkCrawlJobResponse(2L, VkCrawlJobType.GROUP_POSTS, VkCrawlJobStatus.FAILED);
            }
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
