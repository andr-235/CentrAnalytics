package com.ca.centranalytics.integration.channel.vk.repository;

import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VkUserCandidateRepository extends JpaRepository<VkUserCandidate, Long> {
    Optional<VkUserCandidate> findByVkUserId(Long vkUserId);
}
