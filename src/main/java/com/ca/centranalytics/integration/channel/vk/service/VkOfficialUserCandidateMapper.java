package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class VkOfficialUserCandidateMapper {

    private final VkRegionMatcher vkRegionMatcher;

    public VkUserCandidate map(String requestedRegion, VkUserSearchResult result, VkCollectionMethod collectionMethod) {
        VkMatchSource matchSource = vkRegionMatcher
                .match(requestedRegion, result.city(), joinText(result.displayName(), result.firstName(), result.lastName(), result.profileUrl()))
                .orElse(VkMatchSource.FALLBACK);

        return VkUserCandidate.builder()
                .vkUserId(result.id())
                .displayName(result.displayName())
                .firstName(result.firstName())
                .lastName(result.lastName())
                .profileUrl(result.profileUrl())
                .regionMatchSource(matchSource)
                .collectionMethod(collectionMethod)
                .rawJson(result.rawJson())
                .build();
    }

    private String joinText(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}
