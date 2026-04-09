package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.api.SearchVkGroupsRequest;
import com.ca.centranalytics.integration.channel.vk.api.SearchVkUsersRequest;
import com.ca.centranalytics.integration.channel.vk.client.VkFallbackClient;
import com.ca.centranalytics.integration.channel.vk.client.VkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkRegionalCity;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.channel.vk.repository.VkCommentSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkCrawlJobRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkUserCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.service.VkCandidatePersistenceService;
import com.ca.centranalytics.integration.channel.vk.service.VkAuthorNormalizationService;
import com.ca.centranalytics.integration.channel.vk.service.VkCommentIngestionMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkCommentSnapshotMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkCrawlJobService;
import com.ca.centranalytics.integration.channel.vk.service.VkDiscoveryOrchestrator;
import com.ca.centranalytics.integration.channel.vk.service.VkFallbackPolicy;
import com.ca.centranalytics.integration.channel.vk.service.VkOfficialGroupCandidateMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkOfficialUserCandidateMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkPostIngestionMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkRegionMatcher;
import com.ca.centranalytics.integration.channel.vk.service.VkSnapshotPersistenceService;
import com.ca.centranalytics.integration.channel.vk.service.VkSourceNormalizationService;
import com.ca.centranalytics.integration.channel.vk.service.VkWallSnapshotMapper;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VkDiscoveryOrchestratorSearchTest {

    @Test
    void expandsEaoIntoCityQueriesForGroupSearch() {
        SearchHarness harness = new SearchHarness();

        harness.orchestrator.runGroupSearch(
                harness.job(VkCrawlJobType.GROUP_SEARCH),
                new SearchVkGroupsRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.groupCandidates.values())
                .extracting(VkGroupCandidate::getVkGroupId)
                .containsExactly(5001L, 5002L);
        assertThat(harness.job.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(harness.job.getItemCount()).isEqualTo(2);
        assertThat(harness.job.getProcessedCount()).isEqualTo(2);
    }

    @Test
    void filtersOutFallbackGroupsFromRegionalSearch() {
        SearchHarness harness = new SearchHarness();

        harness.orchestrator.runGroupSearch(
                harness.job(VkCrawlJobType.GROUP_SEARCH),
                new SearchVkGroupsRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.groupCandidates.values())
                .extracting(VkGroupCandidate::getVkGroupId)
                .containsExactly(5001L, 5002L);
        assertThat(harness.groupCandidates.values())
                .extracting(VkGroupCandidate::getRegionMatchSource)
                .doesNotContain(VkMatchSource.FALLBACK);
    }

    @Test
    void rotatesRegionalCityBatchesDuringGroupSearch() {
        SearchHarness harness = new SearchHarness();
        harness.regionalSearchTerms = List.of("Биробиджан", "Облучье", "Смидович", "Ленинское", "Амурзет");

        harness.orchestrator.runGroupSearch(
                harness.job(1L, VkCrawlJobType.GROUP_SEARCH),
                new SearchVkGroupsRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.searchedGroupTerms)
                .containsExactly("Биробиджан", "Облучье", "Смидович");

        harness.searchedGroupTerms.clear();
        harness.groupCandidates.clear();

        harness.orchestrator.runGroupSearch(
                harness.job(2L, VkCrawlJobType.GROUP_SEARCH),
                new SearchVkGroupsRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.searchedGroupTerms)
                .containsExactly("Ленинское", "Амурзет");
    }

    @Test
    void expandsEaoIntoCityQueriesForUserSearchWithoutDuplicates() {
        SearchHarness harness = new SearchHarness();

        harness.orchestrator.runUserSearch(
                harness.job(VkCrawlJobType.USER_SEARCH),
                new SearchVkUsersRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.userCandidates.values())
                .extracting(VkUserCandidate::getVkUserId)
                .containsExactly(7001L, 7002L);
        assertThat(harness.job.getStatus()).isEqualTo(VkCrawlJobStatus.COMPLETED);
        assertThat(harness.job.getItemCount()).isEqualTo(2);
        assertThat(harness.job.getProcessedCount()).isEqualTo(2);
    }

    @Test
    void filtersOutFallbackUsersFromRegionalSearch() {
        SearchHarness harness = new SearchHarness();

        harness.orchestrator.runUserSearch(
                harness.job(VkCrawlJobType.USER_SEARCH),
                new SearchVkUsersRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.userCandidates.values())
                .extracting(VkUserCandidate::getVkUserId)
                .containsExactly(7001L, 7002L);
        assertThat(harness.userCandidates.values())
                .extracting(VkUserCandidate::getRegionMatchSource)
                .doesNotContain(VkMatchSource.FALLBACK);
    }

    @Test
    void rotatesRegionalCityBatchesDuringUserSearch() {
        SearchHarness harness = new SearchHarness();
        harness.regionalCities = List.of(
                new VkRegionalCity(1, "Биробиджан"),
                new VkRegionalCity(2, "Облучье"),
                new VkRegionalCity(3, "Смидович"),
                new VkRegionalCity(4, "Ленинское"),
                new VkRegionalCity(5, "Амурзет")
        );

        harness.orchestrator.runUserSearch(
                harness.job(1L, VkCrawlJobType.USER_SEARCH),
                new SearchVkUsersRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.searchedUserCities)
                .containsExactly("Биробиджан", "Облучье", "Смидович");

        harness.searchedUserCities.clear();
        harness.userCandidates.clear();

        harness.orchestrator.runUserSearch(
                harness.job(2L, VkCrawlJobType.USER_SEARCH),
                new SearchVkUsersRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.searchedUserCities)
                .containsExactly("Ленинское", "Амурзет");
    }

    @Test
    void marksGroupPostJobFailedWhenOfficialApiThrows() {
        SearchHarness harness = new SearchHarness();
        harness.failGroupPosts = true;

        harness.orchestrator.runGroupPostCollection(
                harness.job(VkCrawlJobType.GROUP_POSTS),
                5001L,
                new com.ca.centranalytics.integration.channel.vk.api.CollectVkGroupPostsRequest(10, "HYBRID")
        );

        assertThat(harness.job.getStatus()).isEqualTo(VkCrawlJobStatus.FAILED);
        assertThat(harness.job.getErrorCount()).isEqualTo(1);
        assertThat(harness.job.getProcessedCount()).isEqualTo(0);
    }

    @Test
    void marksUserSearchJobFailedWhenOfficialApiThrows() {
        SearchHarness harness = new SearchHarness();
        harness.failUserSearch = true;

        harness.orchestrator.runUserSearch(
                harness.job(VkCrawlJobType.USER_SEARCH),
                new SearchVkUsersRequest("Еврейская автономная область", 10, "HYBRID")
        );

        assertThat(harness.job.getStatus()).isEqualTo(VkCrawlJobStatus.FAILED);
        assertThat(harness.job.getErrorCount()).isEqualTo(1);
        assertThat(harness.job.getProcessedCount()).isEqualTo(0);
    }

    private static final class SearchHarness {
        private final Map<Long, VkGroupCandidate> groupCandidates = new LinkedHashMap<>();
        private final Map<Long, VkUserCandidate> userCandidates = new LinkedHashMap<>();
        private List<String> regionalSearchTerms = List.of("Биробиджан", "Облучье");
        private List<VkRegionalCity> regionalCities = List.of(
                new VkRegionalCity(1, "Биробиджан"),
                new VkRegionalCity(2, "Облучье")
        );
        private final List<String> searchedGroupTerms = new java.util.ArrayList<>();
        private final List<String> searchedUserCities = new java.util.ArrayList<>();
        private boolean failGroupPosts;
        private boolean failUserSearch;
        private VkCrawlJob job;
        private final VkDiscoveryOrchestrator orchestrator;

        private SearchHarness() {
            VkGroupCandidateRepository groupRepository = groupRepository(groupCandidates);
            VkUserCandidateRepository userRepository = userRepository(userCandidates);
            VkCrawlJobRepository crawlJobRepository = crawlJobRepository(this);

            orchestrator = new VkDiscoveryOrchestrator(
                    officialClient(),
                    noopFallbackClient(),
                    new VkOfficialGroupCandidateMapper(new VkRegionMatcher()),
                    new VkOfficialUserCandidateMapper(new VkRegionMatcher()),
                    new VkWallSnapshotMapper(),
                    new VkCommentSnapshotMapper(),
                    new VkCandidatePersistenceService(groupRepository, userRepository),
                    new VkSnapshotPersistenceService(
                            unsupportedRepository(VkWallPostSnapshotRepository.class),
                            unsupportedRepository(VkCommentSnapshotRepository.class)
                    ),
                    new VkFallbackPolicy(),
                    new VkSourceNormalizationService(unsupportedRepository(IntegrationSourceRepository.class)),
                    new VkCrawlJobService(crawlJobRepository, new ObjectMapper()),
                    new VkPostIngestionMapper(new VkAuthorNormalizationService()),
                    new VkCommentIngestionMapper(new VkAuthorNormalizationService()),
                    groupRepository,
                    userRepository,
                    unsupportedRepository(VkWallPostSnapshotRepository.class),
                    null
            );
        }

        private VkCrawlJob job(VkCrawlJobType jobType) {
            return job(1L, jobType);
        }

        private VkCrawlJob job(Long jobId, VkCrawlJobType jobType) {
            this.job = VkCrawlJob.builder()
                    .id(jobId)
                    .jobType(jobType)
                    .status(VkCrawlJobStatus.CREATED)
                    .requestJson("{}")
                    .build();
            return this.job;
        }

        private VkOfficialClient officialClient() {
            return new VkOfficialClient() {
                @Override
                public List<String> resolveRegionalSearchTerms(String region) {
                    if ("Еврейская автономная область".equals(region)) {
                        return regionalSearchTerms;
                    }
                    return List.of(region);
                }

                @Override
                public List<VkRegionalCity> resolveRegionalCities(String region) {
                    if ("Еврейская автономная область".equals(region)) {
                        return regionalCities;
                    }
                    return List.of(new VkRegionalCity(null, region));
                }

                @Override
                public List<VkGroupSearchResult> searchGroups(String region, int limit) {
                    searchedGroupTerms.add(region);
                    return switch (region) {
                        case "Биробиджан" -> List.of(
                                new VkGroupSearchResult(5001L, "Биробиджан Новости", "birobidzhan_news", "Городские новости", "Биробиджан", "{\"id\":5001}"),
                                new VkGroupSearchResult(5002L, "ЕАО Объявления", "eao_ads", "Региональные объявления", "Биробиджан", "{\"id\":5002}"),
                                new VkGroupSearchResult(9001L, "Подслушано Абрамовка", "abramovka_chat", "Чужой регион", null, "{\"id\":9001}")
                        );
                        case "Облучье" -> List.of(
                                new VkGroupSearchResult(5002L, "ЕАО Объявления", "eao_ads", "Региональные объявления", "Облучье", "{\"id\":5002}")
                        );
                        case "Смидович" -> List.of(
                                new VkGroupSearchResult(5003L, "Смидович ЕАО", "smidovich_eao", "Смидовичский район", "Смидович", "{\"id\":5003}")
                        );
                        case "Ленинское" -> List.of(
                                new VkGroupSearchResult(5004L, "Ленинское ЕАО", "leninskoe_eao", "Ленинский район", "Ленинское", "{\"id\":5004}")
                        );
                        case "Амурзет" -> List.of(
                                new VkGroupSearchResult(5005L, "Амурзет ЕАО", "amurzet_eao", "Октябрьский район", "Амурзет", "{\"id\":5005}")
                        );
                        default -> List.of();
                    };
                }

                @Override
                public List<VkUserSearchResult> searchUsers(String region, int limit) {
                    if (failUserSearch) {
                        throw new IllegalStateException("VK SDK call failed");
                    }
                    searchedUserCities.add(region);
                    return switch (region) {
                        case "Биробиджан" -> List.of(
                                new VkUserSearchResult(7001L, "Irina B.", "Irina", "B.", "id7001", "https://vk.com/id7001", "Биробиджан", "Биробиджан", null, 1, null, null, null, null, null, null, null, null, null, null, "{\"id\":7001}"),
                                new VkUserSearchResult(7002L, "Pavel O.", "Pavel", "O.", "id7002", "https://vk.com/id7002", "Облучье", "Облучье", null, 2, null, null, null, null, null, null, null, null, null, null, "{\"id\":7002}"),
                                new VkUserSearchResult(9901L, "Ivan A.", "Ivan", "A.", "id9901", "https://vk.com/id9901", null, null, null, 2, null, null, null, null, null, null, null, null, null, null, "{\"id\":9901}")
                        );
                        case "Облучье" -> List.of(
                                new VkUserSearchResult(7002L, "Pavel O.", "Pavel", "O.", "id7002", "https://vk.com/id7002", "Облучье", "Облучье", null, 2, null, null, null, null, null, null, null, null, null, null, "{\"id\":7002}")
                        );
                        case "Смидович" -> List.of(
                                new VkUserSearchResult(7003L, "S. User", "S.", "User", "id7003", "https://vk.com/id7003", "Смидович", "Смидович", null, 2, null, null, null, null, null, null, null, null, null, null, "{\"id\":7003}")
                        );
                        case "Ленинское" -> List.of(
                                new VkUserSearchResult(7004L, "L. User", "L.", "User", "id7004", "https://vk.com/id7004", "Ленинское", "Ленинское", null, 2, null, null, null, null, null, null, null, null, null, null, "{\"id\":7004}")
                        );
                        case "Амурзет" -> List.of(
                                new VkUserSearchResult(7005L, "A. User", "A.", "User", "id7005", "https://vk.com/id7005", "Амурзет", "Амурзет", null, 2, null, null, null, null, null, null, null, null, null, null, "{\"id\":7005}")
                        );
                        default -> List.of();
                    };
                }

                @Override
                public List<VkUserSearchResult> searchUsers(VkRegionalCity city, int limit) {
                    return searchUsers(city.title(), limit);
                }

                @Override
                public List<VkWallPostResult> getGroupPosts(String domain, int limit) {
                    if (failGroupPosts) {
                        throw new IllegalStateException("VK SDK call failed");
                    }
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

        private VkFallbackClient noopFallbackClient() {
            return new VkFallbackClient() {
                @Override
                public List<VkGroupSearchResult> searchGroups(String region, int limit) {
                    return List.of();
                }

                @Override
                public List<VkUserSearchResult> searchUsers(String region, int limit) {
                    return List.of();
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

        private static VkGroupCandidateRepository groupRepository(Map<Long, VkGroupCandidate> store) {
            return (VkGroupCandidateRepository) Proxy.newProxyInstance(
                    VkGroupCandidateRepository.class.getClassLoader(),
                    new Class[]{VkGroupCandidateRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByVkGroupId" -> Optional.ofNullable(store.get((Long) args[0]));
                        case "save" -> {
                            VkGroupCandidate candidate = (VkGroupCandidate) args[0];
                            store.put(candidate.getVkGroupId(), candidate);
                            yield candidate;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private static VkUserCandidateRepository userRepository(Map<Long, VkUserCandidate> store) {
            return (VkUserCandidateRepository) Proxy.newProxyInstance(
                    VkUserCandidateRepository.class.getClassLoader(),
                    new Class[]{VkUserCandidateRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByVkUserId" -> Optional.ofNullable(store.get((Long) args[0]));
                        case "save" -> {
                            VkUserCandidate candidate = (VkUserCandidate) args[0];
                            store.put(candidate.getVkUserId(), candidate);
                            yield candidate;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private static VkCrawlJobRepository crawlJobRepository(SearchHarness harness) {
            return (VkCrawlJobRepository) Proxy.newProxyInstance(
                    VkCrawlJobRepository.class.getClassLoader(),
                    new Class[]{VkCrawlJobRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findById" -> Optional.ofNullable(harness.job);
                        case "save" -> {
                            harness.job = (VkCrawlJob) args[0];
                            yield harness.job;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        @SuppressWarnings("unchecked")
        private static <T> T unsupportedRepository(Class<T> repositoryType) {
            return (T) Proxy.newProxyInstance(
                    repositoryType.getClassLoader(),
                    new Class[]{repositoryType},
                    (proxy, method, args) -> {
                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
