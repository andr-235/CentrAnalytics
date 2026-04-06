package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import com.ca.centranalytics.integration.domain.entity.IntegrationStatus;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VkSourceNormalizationService {

    private final IntegrationSourceRepository integrationSourceRepository;

    public IntegrationSource resolveGroupSource(Long groupId, String groupName) {
        String sourceExternalId = String.valueOf(groupId);
        String resolvedName = sourceName(groupId, groupName);
        String settingsJson = settingsJson(groupId);

        return integrationSourceRepository.findByPlatformAndSourceExternalId(Platform.VK, sourceExternalId)
                .map(existing -> {
                    existing.setName(resolvedName);
                    existing.setStatus(IntegrationStatus.ACTIVE);
                    existing.setSettingsJson(settingsJson);
                    return integrationSourceRepository.save(existing);
                })
                .orElseGet(() -> integrationSourceRepository.save(IntegrationSource.builder()
                        .platform(Platform.VK)
                        .sourceExternalId(sourceExternalId)
                        .name(resolvedName)
                        .status(IntegrationStatus.ACTIVE)
                        .settingsJson(settingsJson)
                        .build()));
    }

    public VkGroupCandidate placeholderGroupCandidate(Long groupId, String groupName, VkCollectionMethod collectionMethod) {
        return VkGroupCandidate.builder()
                .vkGroupId(groupId)
                .name(sourceName(groupId, groupName))
                .regionMatchSource(VkMatchSource.FALLBACK)
                .collectionMethod(collectionMethod)
                .rawJson("{\"id\":" + groupId + "}")
                .build();
    }

    public String sourceName(Long groupId, String groupName) {
        return StringUtils.hasText(groupName) ? groupName : "VK group " + groupId;
    }

    public String settingsJson(Long groupId) {
        return "{\"groupId\":" + groupId + "}";
    }
}
