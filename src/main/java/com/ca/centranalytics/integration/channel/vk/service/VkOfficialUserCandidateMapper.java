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
                .match(
                        requestedRegion,
                        firstNonBlank(result.city(), result.homeTown()),
                        joinText(
                                result.displayName(),
                                result.firstName(),
                                result.lastName(),
                                result.username(),
                                result.profileUrl(),
                                result.status(),
                                result.site(),
                                result.education()
                        )
                )
                .orElse(VkMatchSource.FALLBACK);

        return VkUserCandidate.builder()
                .vkUserId(result.id())
                .displayName(result.displayName())
                .firstName(result.firstName())
                .lastName(result.lastName())
                .username(result.username())
                .profileUrl(result.profileUrl())
                .city(result.city())
                .homeTown(result.homeTown())
                .birthDate(result.birthDate())
                .sex(result.sex())
                .status(result.status())
                .lastSeenAt(result.lastSeenAt())
                .avatarUrl(result.avatarUrl())
                .mobilePhone(result.mobilePhone())
                .homePhone(result.homePhone())
                .site(result.site())
                .relation(result.relation())
                .education(result.education())
                .careerJson(result.careerJson())
                .countersJson(result.countersJson())
                .regionMatchSource(matchSource)
                .collectionMethod(collectionMethod)
                .rawJson(result.rawJson())
                .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String joinText(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}
