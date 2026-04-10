package com.ca.centranalytics.integration.channel.vk.repository;

import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VkGroupCandidateRepository extends JpaRepository<VkGroupCandidate, Long> {
    Optional<VkGroupCandidate> findByVkGroupId(Long vkGroupId);
    Optional<VkGroupCandidate> findByScreenNameIgnoreCase(String screenName);
    List<VkGroupCandidate> findAllByOrderByUpdatedAtDesc();
    long countBySourceId(Long sourceId);
}
