package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.VkCommentSnapshotResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkGroupCandidateResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkUserCandidateResponse;
import com.ca.centranalytics.integration.channel.vk.api.VkWallPostSnapshotResponse;
import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import com.ca.centranalytics.integration.channel.vk.repository.VkCommentSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkUserCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VkDiscoveryQueryService {

    private final VkGroupCandidateRepository vkGroupCandidateRepository;
    private final VkUserCandidateRepository vkUserCandidateRepository;
    private final VkWallPostSnapshotRepository vkWallPostSnapshotRepository;
    private final VkCommentSnapshotRepository vkCommentSnapshotRepository;

    public List<VkGroupCandidateResponse> getGroups(String search) {
        String normalizedSearch = normalize(search);
        return vkGroupCandidateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(group -> normalizedSearch == null || contains(group.getName(), normalizedSearch) || contains(group.getScreenName(), normalizedSearch))
                .map(this::toGroupResponse)
                .toList();
    }

    public List<VkUserCandidateResponse> getUsers(String search) {
        String normalizedSearch = normalize(search);
        return vkUserCandidateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(user -> normalizedSearch == null
                        || contains(user.getDisplayName(), normalizedSearch)
                        || contains(user.getFirstName(), normalizedSearch)
                        || contains(user.getLastName(), normalizedSearch)
                        || contains(user.getUsername(), normalizedSearch)
                        || contains(user.getCity(), normalizedSearch)
                        || contains(user.getHomeTown(), normalizedSearch))
                .map(this::toUserResponse)
                .toList();
    }

    public List<VkWallPostSnapshotResponse> getGroupPosts(Long groupId) {
        return vkWallPostSnapshotRepository.findAllByOwnerIdOrderByUpdatedAtDesc(-Math.abs(groupId)).stream()
                .map(this::toPostResponse)
                .toList();
    }

    public List<VkCommentSnapshotResponse> getPostComments(Long ownerId, Long postId) {
        return vkCommentSnapshotRepository.findAllByOwnerIdAndPostIdOrderByUpdatedAtDesc(ownerId, postId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private VkGroupCandidateResponse toGroupResponse(VkGroupCandidate group) {
        return new VkGroupCandidateResponse(
                group.getId(),
                group.getVkGroupId(),
                group.getSource() != null ? group.getSource().getId() : null,
                group.getScreenName(),
                group.getName(),
                group.getRegionMatchSource(),
                group.getCollectionMethod(),
                group.getRawJson(),
                group.getUpdatedAt()
        );
    }

    private VkUserCandidateResponse toUserResponse(VkUserCandidate user) {
        return new VkUserCandidateResponse(
                user.getId(),
                user.getVkUserId(),
                user.getSource() != null ? user.getSource().getId() : null,
                user.getDisplayName(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getProfileUrl(),
                user.getCity(),
                user.getHomeTown(),
                user.getBirthDate(),
                user.getSex(),
                user.getStatus(),
                user.getLastSeenAt(),
                user.getAvatarUrl(),
                user.getMobilePhone(),
                user.getHomePhone(),
                user.getSite(),
                user.getRelation(),
                user.getEducation(),
                user.getCareerJson(),
                user.getCountersJson(),
                user.getRegionMatchSource(),
                user.getCollectionMethod(),
                user.getRawJson(),
                user.getUpdatedAt()
        );
    }

    private VkWallPostSnapshotResponse toPostResponse(VkWallPostSnapshot post) {
        return new VkWallPostSnapshotResponse(
                post.getId(),
                post.getOwnerId(),
                post.getPostId(),
                post.getSource() != null ? post.getSource().getId() : null,
                post.getAuthorVkUserId(),
                post.getText(),
                post.getCollectionMethod(),
                post.getRawJson(),
                post.getUpdatedAt()
        );
    }

    private VkCommentSnapshotResponse toCommentResponse(VkCommentSnapshot comment) {
        return new VkCommentSnapshotResponse(
                comment.getId(),
                comment.getOwnerId(),
                comment.getPostId(),
                comment.getCommentId(),
                comment.getSource() != null ? comment.getSource().getId() : null,
                comment.getAuthorVkUserId(),
                comment.getText(),
                comment.getCollectionMethod(),
                comment.getRawJson(),
                comment.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
