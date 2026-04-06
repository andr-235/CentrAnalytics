package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkUserCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VkCandidatePersistenceService {

    private final VkGroupCandidateRepository vkGroupCandidateRepository;
    private final VkUserCandidateRepository vkUserCandidateRepository;

    public VkGroupCandidate upsertGroupCandidate(VkGroupCandidate candidate) {
        return vkGroupCandidateRepository.findByVkGroupId(candidate.getVkGroupId())
                .map(existing -> {
                    existing.setSource(candidate.getSource());
                    existing.setScreenName(candidate.getScreenName());
                    existing.setName(candidate.getName());
                    existing.setRegionMatchSource(candidate.getRegionMatchSource());
                    existing.setCollectionMethod(candidate.getCollectionMethod());
                    existing.setRawJson(candidate.getRawJson());
                    return vkGroupCandidateRepository.save(existing);
                })
                .orElseGet(() -> vkGroupCandidateRepository.save(candidate));
    }

    public VkUserCandidate upsertUserCandidate(VkUserCandidate candidate) {
        return vkUserCandidateRepository.findByVkUserId(candidate.getVkUserId())
                .map(existing -> {
                    existing.setSource(candidate.getSource());
                    existing.setDisplayName(candidate.getDisplayName());
                    existing.setFirstName(candidate.getFirstName());
                    existing.setLastName(candidate.getLastName());
                    existing.setProfileUrl(candidate.getProfileUrl());
                    existing.setRegionMatchSource(candidate.getRegionMatchSource());
                    existing.setCollectionMethod(candidate.getCollectionMethod());
                    existing.setRawJson(candidate.getRawJson());
                    return vkUserCandidateRepository.save(existing);
                })
                .orElseGet(() -> vkUserCandidateRepository.save(candidate));
    }
}
