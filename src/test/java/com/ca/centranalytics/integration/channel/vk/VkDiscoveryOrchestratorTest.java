package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.TestcontainersConfiguration;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest;
import com.ca.centranalytics.integration.channel.vk.api.CollectVkPostCommentsRequest;
import com.ca.centranalytics.integration.channel.vk.api.EnrichVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.client.VkFallbackClient;
import com.ca.centranalytics.integration.channel.vk.client.VkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import com.ca.centranalytics.integration.channel.vk.repository.VkCommentSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkCrawlJobRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkUserCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.service.VkCandidatePersistenceService;
import com.ca.centranalytics.integration.channel.vk.service.VkDiscoveryOrchestrator;
import com.ca.centranalytics.integration.channel.vk.service.VkFallbackPolicy;
import com.ca.centranalytics.integration.channel.vk.service.VkOfficialGroupCandidateMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkOfficialUserCandidateMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkRegionMatcher;
import com.ca.centranalytics.integration.domain.repository.ConversationRepository;
import com.ca.centranalytics.integration.domain.repository.ExternalUserRepository;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import com.ca.centranalytics.integration.domain.repository.MessageAttachmentRepository;
import com.ca.centranalytics.integration.domain.repository.MessageRepository;
import com.ca.centranalytics.integration.domain.repository.RawEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import({TestcontainersConfiguration.class, VkDiscoveryOrchestratorTest.TestConfig.class})
class VkDiscoveryOrchestratorTest {

    @Autowired
    private VkDiscoveryOrchestrator vkDiscoveryOrchestrator;

    @Autowired
    private VkCrawlJobRepository vkCrawlJobRepository;

    @Autowired
    private VkGroupCandidateRepository vkGroupCandidateRepository;

    @Autowired
    private VkUserCandidateRepository vkUserCandidateRepository;

    @Autowired
    private VkWallPostSnapshotRepository vkWallPostSnapshotRepository;

    @Autowired
    private VkCommentSnapshotRepository vkCommentSnapshotRepository;

    @Autowired
    private IntegrationSourceRepository integrationSourceRepository;

    @Autowired
    private ExternalUserRepository externalUserRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;

    @Autowired
    private RawEventRepository rawEventRepository;

    @BeforeEach
    void setUp() {
        messageAttachmentRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        externalUserRepository.deleteAll();
        rawEventRepository.deleteAll();
        integrationSourceRepository.deleteAll();
        vkCommentSnapshotRepository.deleteAll();
        vkWallPostSnapshotRepository.deleteAll();
        vkUserCandidateRepository.deleteAll();
        vkGroupCandidateRepository.deleteAll();
        vkCrawlJobRepository.deleteAll();
    }

    @Test
    void completesGroupSearchWithOfficialResults() {
        VkCrawlJob job = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.GROUP_SEARCH)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{}")
                .build());

        vkDiscoveryOrchestrator.runGroupSearch(job, new SearchVkGroupsRequest("Primorsky Krai", 25, "HYBRID"));

        VkCrawlJob persisted = vkCrawlJobRepository.findById(job.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(persisted.getItemCount()).isEqualTo(1);
        assertThat(persisted.getProcessedCount()).isEqualTo(1);
        assertThat(vkGroupCandidateRepository.findAll()).hasSize(1);
    }

    @Test
    void fallsBackToFallbackClientWhenOfficialUserSearchReturnsNothing() {
        VkCrawlJob job = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.USER_SEARCH)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{}")
                .build());

        vkDiscoveryOrchestrator.runUserSearch(job, new SearchVkUsersRequest("Primorsky Krai", 25, "HYBRID"));

        VkCrawlJob persisted = vkCrawlJobRepository.findById(job.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(persisted.getItemCount()).isEqualTo(1);
        assertThat(vkUserCandidateRepository.findAll()).hasSize(1);
    }

    @Test
    void expandsEaoIntoCityQueriesForGroupSearch() {
        VkCrawlJob job = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.GROUP_SEARCH)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{}")
                .build());

        vkDiscoveryOrchestrator.runGroupSearch(job, new SearchVkGroupsRequest("Еврейская автономная область", 10, "HYBRID"));

        VkCrawlJob persisted = vkCrawlJobRepository.findById(job.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(persisted.getItemCount()).isEqualTo(2);
        assertThat(vkGroupCandidateRepository.findAll())
                .extracting(candidate -> candidate.getVkGroupId())
                .containsExactlyInAnyOrder(5001L, 5002L);
    }

    @Test
    void expandsEaoIntoCityQueriesForUserSearchWithoutDuplicates() {
        VkCrawlJob job = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.USER_SEARCH)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{}")
                .build());

        vkDiscoveryOrchestrator.runUserSearch(job, new SearchVkUsersRequest("Еврейская автономная область", 10, "HYBRID"));

        VkCrawlJob persisted = vkCrawlJobRepository.findById(job.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(persisted.getItemCount()).isEqualTo(2);
        assertThat(vkUserCandidateRepository.findAll())
                .extracting(candidate -> candidate.getVkUserId())
                .containsExactlyInAnyOrder(7001L, 7002L);
    }

    @Test
    void collectsGroupPostsAndIngestsThemIntoIntegrationDomain() {
        VkCrawlJob job = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.GROUP_POSTS)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{}")
                .build());

        vkDiscoveryOrchestrator.runGroupPostCollection(job, 1001L, new CollectVkGroupPostsRequest(25, "HYBRID"));

        VkCrawlJob persisted = vkCrawlJobRepository.findById(job.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(persisted.getProcessedCount()).isEqualTo(1);
        assertThat(vkWallPostSnapshotRepository.findAll()).hasSize(1);
        assertThat(integrationSourceRepository.findAll()).singleElement()
                .extracting(source -> source.getSourceExternalId())
                .isEqualTo("1001");
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(rawEventRepository.findAll()).hasSize(1);
    }

    @Test
    void collectsPostCommentsAndIngestsThemIntoIntegrationDomain() {
        VkCrawlJob postsJob = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.GROUP_POSTS)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{}")
                .build());
        vkDiscoveryOrchestrator.runGroupPostCollection(postsJob, 1001L, new CollectVkGroupPostsRequest(25, "HYBRID"));

        VkCrawlJob commentsJob = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.POST_COMMENTS)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{}")
                .build());

        vkDiscoveryOrchestrator.runPostCommentCollection(
                commentsJob,
                new CollectVkPostCommentsRequest(List.of(3003L), 25, "HYBRID")
        );

        VkCrawlJob persisted = vkCrawlJobRepository.findById(commentsJob.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(persisted.getProcessedCount()).isEqualTo(1);
        assertThat(vkCommentSnapshotRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).hasSize(2);
        assertThat(conversationRepository.findAll()).hasSize(2);
        assertThat(rawEventRepository.findAll()).hasSize(2);
    }

    @Test
    void enrichesUsersByIds() {
        VkCrawlJob job = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.AUTHOR_PROFILE_ENRICH)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{}")
                .build());

        vkDiscoveryOrchestrator.runUserEnrichment(job, new EnrichVkUsersRequest(List.of(2002L), "HYBRID"));

        VkCrawlJob persisted = vkCrawlJobRepository.findById(job.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(persisted.getProcessedCount()).isEqualTo(1);
        assertThat(vkUserCandidateRepository.findByVkUserId(2002L)).isPresent();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        VkOfficialClient vkOfficialClient() {
            return new VkOfficialClient() {
                @Override
                public List<String> resolveRegionalSearchTerms(String region) {
                    if ("Еврейская автономная область".equals(region)) {
                        return List.of("Биробиджан", "Облучье");
                    }
                    return List.of(region);
                }

                @Override
                public List<VkGroupSearchResult> searchGroups(String region, int limit) {
                    if ("Биробиджан".equals(region)) {
                        return List.of(
                                new VkGroupSearchResult(
                                        5001L,
                                        "Биробиджан Новости",
                                        "birobidzhan_news",
                                        "Городские новости",
                                        "Биробиджан",
                                        "{\"id\":5001}"
                                ),
                                new VkGroupSearchResult(
                                        5002L,
                                        "ЕАО Объявления",
                                        "eao_ads",
                                        "Региональные объявления",
                                        "Биробиджан",
                                        "{\"id\":5002}"
                                )
                        );
                    }
                    if ("Облучье".equals(region)) {
                        return List.of(
                                new VkGroupSearchResult(
                                        5002L,
                                        "ЕАО Объявления",
                                        "eao_ads",
                                        "Региональные объявления",
                                        "Биробиджан",
                                        "{\"id\":5002}"
                                )
                        );
                    }
                    return List.of(new VkGroupSearchResult(
                            1001L,
                            "Primorye Group",
                            "primorye_group",
                            "News from Primorsky Krai",
                            "Vladivostok",
                            "{\"id\":1001}"
                    ));
                }

                @Override
                public List<VkUserSearchResult> searchUsers(String region, int limit) {
                    if ("Биробиджан".equals(region)) {
                        return List.of(
                                new VkUserSearchResult(
                                        7001L,
                                        "Irina B.",
                                        "Irina",
                                        "B.",
                                        "id7001",
                                        "https://vk.com/id7001",
                                        "Биробиджан",
                                        "Биробиджан",
                                        null,
                                        1,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{\"id\":7001}"
                                ),
                                new VkUserSearchResult(
                                        7002L,
                                        "Pavel O.",
                                        "Pavel",
                                        "O.",
                                        "id7002",
                                        "https://vk.com/id7002",
                                        "Облучье",
                                        "Облучье",
                                        null,
                                        2,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{\"id\":7002}"
                                )
                        );
                    }
                    if ("Облучье".equals(region)) {
                        return List.of(new VkUserSearchResult(
                                7002L,
                                "Pavel O.",
                                "Pavel",
                                "O.",
                                "id7002",
                                "https://vk.com/id7002",
                                "Облучье",
                                "Облучье",
                                null,
                                2,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "{\"id\":7002}"
                        ));
                    }
                    return List.of();
                }

                @Override
                public List<VkWallPostResult> getGroupPosts(String domain, int limit) {
                    long groupId = 1001L;
                    return List.of(new VkWallPostResult(
                            -groupId,
                            3003L,
                            2002L,
                            "Hello from Primorye",
                            null,
                            "{\"owner_id\":-" + groupId + ",\"id\":3003}"
                    ));
                }

                @Override
                public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
                    return List.of(new VkCommentResult(
                            ownerId,
                            postId,
                            4004L,
                            3003L,
                            "Great post",
                            null,
                            "{\"owner_id\":" + ownerId + ",\"post_id\":" + postId + ",\"id\":4004}"
                    ));
                }

                @Override
                public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
                    return userIds.stream()
                            .map(userId -> switch (userId.intValue()) {
                                case 2002 -> new VkUserSearchResult(
                                        2002L,
                                        "Ivan Ivanov",
                                        "Ivan",
                                        "Ivanov",
                                        "id2002",
                                        "https://vk.com/id2002",
                                        "Primorsky Krai",
                                        "Vladivostok",
                                        "10.10.1990",
                                        2,
                                        "online",
                                        null,
                                        "https://vk.com/images/2002.jpg",
                                        "+79990000001",
                                        null,
                                        null,
                                        1,
                                        "FEFU",
                                        "{\"company\":\"CA\"}",
                                        "{\"friends\":120}",
                                        "{\"id\":2002}"
                                );
                                case 3003 -> new VkUserSearchResult(
                                        3003L,
                                        "Petr Petrov",
                                        "Petr",
                                        "Petrov",
                                        "id3003",
                                        "https://vk.com/id3003",
                                        "Primorsky Krai",
                                        "Artem",
                                        null,
                                        2,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{\"followers\":2}",
                                        "{\"id\":3003}"
                                );
                                default -> null;
                            })
                            .filter(java.util.Objects::nonNull)
                            .toList();
                }
            };
        }

        @Bean
        @Primary
        VkFallbackClient vkFallbackClient() {
            return new VkFallbackClient() {
                @Override
                public List<VkGroupSearchResult> searchGroups(String region, int limit) {
                    return List.of();
                }

                @Override
                public List<VkUserSearchResult> searchUsers(String region, int limit) {
                    return List.of(new VkUserSearchResult(
                            2002L,
                            "Ivan Ivanov",
                            "Ivan",
                            "Ivanov",
                            "id2002",
                            "https://vk.com/id2002",
                            "Primorsky Krai",
                            "Vladivostok",
                            "10.10.1990",
                            2,
                            "online",
                            null,
                            "https://vk.com/images/2002.jpg",
                            "+79990000001",
                            null,
                            null,
                            1,
                            "FEFU",
                            "{\"company\":\"CA\"}",
                            "{\"friends\":120}",
                            "{\"id\":2002}"
                    ));
                }

                @Override
                public List<VkWallPostResult> getGroupPosts(Long groupId, int limit) {
                    return List.of();
                }

                @Override
                public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
                    return List.of();
                }

                @Override
                public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
                    return List.of();
                }
            };
        }

        @Bean
        VkRegionMatcher vkRegionMatcher() {
            return new VkRegionMatcher();
        }

        @Bean
        VkOfficialGroupCandidateMapper vkOfficialGroupCandidateMapper(VkRegionMatcher vkRegionMatcher) {
            return new VkOfficialGroupCandidateMapper(vkRegionMatcher);
        }

        @Bean
        VkOfficialUserCandidateMapper vkOfficialUserCandidateMapper(VkRegionMatcher vkRegionMatcher) {
            return new VkOfficialUserCandidateMapper(vkRegionMatcher);
        }

        @Bean
        VkFallbackPolicy vkFallbackPolicy() {
            return new VkFallbackPolicy();
        }

        @Bean
        VkCandidatePersistenceService vkCandidatePersistenceService(
                VkGroupCandidateRepository vkGroupCandidateRepository,
                VkUserCandidateRepository vkUserCandidateRepository
        ) {
            return new VkCandidatePersistenceService(vkGroupCandidateRepository, vkUserCandidateRepository);
        }
    }
}
