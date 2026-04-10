package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.service.VkGroupIdentifierResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VkGroupIdentifierResolverTest {

    @Test
    void resolvesCandidateIdVkGroupIdAliasesScreenNameAndUrlsAgainstExistingGroupsOnly() {
        Map<Long, VkGroupCandidate> groupsById = new LinkedHashMap<>();
        groupsById.put(11L, VkGroupCandidate.builder()
                .id(11L)
                .vkGroupId(1001L)
                .screenName("primorye_group")
                .name("Primorye Group")
                .regionMatchSource(VkMatchSource.TEXT)
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"id\":1001}")
                .build());

        VkGroupIdentifierResolver resolver = new VkGroupIdentifierResolver(groupRepository(groupsById));

        assertThat(resolver.resolve(List.of(
                "11",
                "1001",
                "primorye_group",
                "club1001",
                "public1001",
                "https://vk.com/primorye_group",
                "https://vk.com/club1001",
                "missing_group"
        )).resolvedGroups())
                .extracting(VkGroupCandidate::getVkGroupId)
                .containsExactly(1001L);
        assertThat(resolver.resolve(List.of("missing_group")).unresolvedIdentifiers())
                .containsExactly("missing_group");
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
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
