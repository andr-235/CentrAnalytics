package com.ca.centranalytics.integration.channel.vk.repository;

import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VkCommentSnapshotRepository extends JpaRepository<VkCommentSnapshot, Long> {
    Optional<VkCommentSnapshot> findByOwnerIdAndPostIdAndCommentId(Long ownerId, Long postId, Long commentId);
    List<VkCommentSnapshot> findAllByOwnerIdAndPostIdOrderByUpdatedAtDesc(Long ownerId, Long postId);
    void deleteAllByOwnerId(Long ownerId);
}
