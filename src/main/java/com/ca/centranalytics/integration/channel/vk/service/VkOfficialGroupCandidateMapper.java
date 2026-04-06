package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class VkOfficialGroupCandidateMapper {

    private final VkRegionMatcher vkRegionMatcher;

    public VkGroupCandidate map(String requestedRegion, VkGroupSearchResult result, VkCollectionMethod collectionMethod) {
        VkMatchSource matchSource = vkRegionMatcher
                .match(requestedRegion, result.city(), joinText(result.name(), result.description(), result.screenName()))
                .orElse(VkMatchSource.FALLBACK);

        return VkGroupCandidate.builder()
                .vkGroupId(result.id())
                .screenName(result.screenName())
                .name(result.name())
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
