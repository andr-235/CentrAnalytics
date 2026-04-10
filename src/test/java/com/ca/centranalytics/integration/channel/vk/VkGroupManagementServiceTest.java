package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobResponse;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import com.ca.centranalytics.integration.channel.vk.repository.VkCommentSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.service.VkCrawlCommandService;
import com.ca.centranalytics.integration.channel.vk.service.VkGroupIdentifierResolver;
import com.ca.centranalytics.integration.channel.vk.service.VkGroupManagementService;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import com.ca.centranalytics.integration.domain.entity.IntegrationStatus;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VkGroupManagementServiceTest {

    @Test
    void collectsPostsAndCommentsForResolvedExistingGroupsOnly() {
        Fixture fixture = new Fixture();

        var response = fixture.service.collectGroups(List.of("1001", "primorye_group", "missing_group"), 10, 5, 20, "HYBRID");

        assertThat(response.resolvedGroups())
                .extracting(group -> group.vkGroupId())
                .containsExactly(1001L);
        assertThat(response.unresolvedIdentifiers())
                .containsExactly("missing_group");
        assertThat(fixture.crawlCommandService.postRequests)
                .containsExactly(new GroupPostCall(1001L, new CollectVkGroupPostsRequest(10, "HYBRID")));
        assertThat(fixture.crawlCommandService.commentRequests)
                .containsExactly(new CollectVkPostCommentsRequest(List.of(3003L, 3004L), 20, "HYBRID"));
    }

    @Test
    void deletesResolvedGroupsAndRelatedSnapshotsAndSources() {
        Fixture fixture = new Fixture();

        var response = fixture.service.deleteGroups(List.of("1001", "primorye_group", "missing_group"));

        assertThat(response.deletedGroups())
                .extracting(group -> group.vkGroupId())
                .containsExactly(1001L);
        assertThat(response.unresolvedIdentifiers())
                .containsExactly("missing_group");
        assertThat(fixture.groupsById).isEmpty();
        assertThat(fixture.postsByOwner).isEmpty();
        assertThat(fixture.deletedCommentOwners).containsExactly(-1001L);
        assertThat(fixture.deletedSourceIds).containsExactly(501L);
    }

    private static final class Fixture {
        private final Map<Long, VkGroupCandidate> groupsById = new LinkedHashMap<>();
        private final Map<Long, List<VkWallPostSnapshot>> postsByOwner = new LinkedHashMap<>();
        private final List<Long> deletedCommentOwners = new ArrayList<>();
        private final List<Long> deletedSourceIds = new ArrayList<>();
        private final RecordingVkCrawlCommandService crawlCommandService = new RecordingVkCrawlCommandService();
        private final VkGroupManagementService service;

        private Fixture() {
            IntegrationSource source = IntegrationSource.builder()
                    .id(501L)
                    .platform(Platform.VK)
                    .sourceExternalId("1001")
                    .name("Primorye Group")
                    .status(IntegrationStatus.ACTIVE)
                    .settingsJson("{\"groupId\":1001}")
                    .build();
            groupsById.put(11L, VkGroupCandidate.builder()
                    .id(11L)
                    .vkGroupId(1001L)
                    .screenName("primorye_group")
                    .name("Primorye Group")
                    .source(source)
                    .regionMatchSource(VkMatchSource.TEXT)
                    .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                    .rawJson("{\"id\":1001}")
                    .build());
            postsByOwner.put(-1001L, List.of(
                    VkWallPostSnapshot.builder().ownerId(-1001L).postId(3003L).build(),
                    VkWallPostSnapshot.builder().ownerId(-1001L).postId(3004L).build()
            ));

            VkGroupCandidateRepository groupRepository = groupRepository(groupsById);
            VkWallPostSnapshotRepository wallRepository = wallRepository(postsByOwner);
            VkCommentSnapshotRepository commentRepository = commentRepository(deletedCommentOwners);
            IntegrationSourceRepository sourceRepository = sourceRepository(groupsById, deletedSourceIds);

            service = new VkGroupManagementService(
                    new VkGroupIdentifierResolver(groupRepository),
                    crawlCommandService,
                    wallRepository,
                    commentRepository,
                    groupRepository,
                    sourceRepository
            );
        }
    }

    private static VkGroupCandidateRepository groupRepository(Map<Long, VkGroupCandidate> groupsById) {
        return (VkGroupCandidateRepository) Proxy.newProxyInstance(
                VkGroupCandidateRepository.class.getClassLoader(),
                new Class[]{VkGroupCandidateRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.ofNullable(groupsById.get((Long) args[0]));
                    case "findByVkGroupId" -> groupsById.values().stream()
                            .filter(candidate -> candidate.getVkGroupId().equals(args[0]))
                            .findFirst();
                    case "findByScreenNameIgnoreCase" -> groupsById.values().stream()
                            .filter(candidate -> candidate.getScreenName() != null)
                            .filter(candidate -> candidate.getScreenName().equalsIgnoreCase((String) args[0]))
                            .findFirst();
                    case "delete" -> {
                        VkGroupCandidate candidate = (VkGroupCandidate) args[0];
                        groupsById.remove(candidate.getId());
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static VkWallPostSnapshotRepository wallRepository(Map<Long, List<VkWallPostSnapshot>> postsByOwner) {
        return (VkWallPostSnapshotRepository) Proxy.newProxyInstance(
                VkWallPostSnapshotRepository.class.getClassLoader(),
                new Class[]{VkWallPostSnapshotRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByOwnerIdOrderByUpdatedAtDesc" -> postsByOwner.getOrDefault((Long) args[0], List.of());
                    case "deleteAllByOwnerId" -> {
                        postsByOwner.remove((Long) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static VkCommentSnapshotRepository commentRepository(List<Long> deletedCommentOwners) {
        return (VkCommentSnapshotRepository) Proxy.newProxyInstance(
                VkCommentSnapshotRepository.class.getClassLoader(),
                new Class[]{VkCommentSnapshotRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "deleteAllByOwnerId" -> {
                        deletedCommentOwners.add((Long) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static IntegrationSourceRepository sourceRepository(Map<Long, VkGroupCandidate> groupsById, List<Long> deletedSourceIds) {
        return (IntegrationSourceRepository) Proxy.newProxyInstance(
                IntegrationSourceRepository.class.getClassLoader(),
                new Class[]{IntegrationSourceRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "delete" -> {
                        IntegrationSource source = (IntegrationSource) args[0];
                        deletedSourceIds.add(source.getId());
                        groupsById.entrySet().removeIf(entry -> entry.getValue().getSource() != null
                                && entry.getValue().getSource().getId().equals(source.getId()));
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static final class RecordingVkCrawlCommandService extends VkCrawlCommandService {
        private final List<GroupPostCall> postRequests = new ArrayList<>();
        private final List<CollectVkPostCommentsRequest> commentRequests = new ArrayList<>();

        private RecordingVkCrawlCommandService() {
            super(null, null);
        }

        @Override
        public VkCrawlJobResponse createGroupPostsJob(Long groupId, CollectVkGroupPostsRequest request) {
            postRequests.add(new GroupPostCall(groupId, request));
            return new VkCrawlJobResponse(1L, VkCrawlJobType.GROUP_POSTS, VkCrawlJobStatus.COMPLETED);
        }

        @Override
        public VkCrawlJobResponse createPostCommentsJob(CollectVkPostCommentsRequest request) {
            commentRequests.add(request);
            return new VkCrawlJobResponse(2L, VkCrawlJobType.POST_COMMENTS, VkCrawlJobStatus.COMPLETED);
        }
    }

    private record GroupPostCall(Long groupId, CollectVkGroupPostsRequest request) {
    }
}
