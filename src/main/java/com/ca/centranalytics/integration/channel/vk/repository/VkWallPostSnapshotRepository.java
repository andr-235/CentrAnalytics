package com.ca.centranalytics.integration.channel.vk.repository;

import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VkWallPostSnapshotRepository extends JpaRepository<VkWallPostSnapshot, Long> {
    Optional<VkWallPostSnapshot> findByOwnerIdAndPostId(Long ownerId, Long postId);
    List<VkWallPostSnapshot> findAllByPostIdIn(List<Long> postIds);
    List<VkWallPostSnapshot> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
