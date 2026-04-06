package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import com.ca.centranalytics.integration.channel.vk.repository.VkCommentSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VkSnapshotPersistenceService {

    private final VkWallPostSnapshotRepository vkWallPostSnapshotRepository;
    private final VkCommentSnapshotRepository vkCommentSnapshotRepository;

    public VkWallPostSnapshot upsertWallPostSnapshot(VkWallPostSnapshot snapshot) {
        return vkWallPostSnapshotRepository.findByOwnerIdAndPostId(snapshot.getOwnerId(), snapshot.getPostId())
                .map(existing -> {
                    existing.setSource(snapshot.getSource());
                    existing.setAuthorVkUserId(snapshot.getAuthorVkUserId());
                    existing.setText(snapshot.getText());
                    existing.setCollectionMethod(snapshot.getCollectionMethod());
                    existing.setRawJson(snapshot.getRawJson());
                    return vkWallPostSnapshotRepository.save(existing);
                })
                .orElseGet(() -> vkWallPostSnapshotRepository.save(snapshot));
    }

    public VkCommentSnapshot upsertCommentSnapshot(VkCommentSnapshot snapshot) {
        return vkCommentSnapshotRepository.findByOwnerIdAndPostIdAndCommentId(
                        snapshot.getOwnerId(),
                        snapshot.getPostId(),
                        snapshot.getCommentId()
                )
                .map(existing -> {
                    existing.setSource(snapshot.getSource());
                    existing.setAuthorVkUserId(snapshot.getAuthorVkUserId());
                    existing.setText(snapshot.getText());
                    existing.setCollectionMethod(snapshot.getCollectionMethod());
                    existing.setRawJson(snapshot.getRawJson());
                    return vkCommentSnapshotRepository.save(existing);
                })
                .orElseGet(() -> vkCommentSnapshotRepository.save(snapshot));
    }
}
