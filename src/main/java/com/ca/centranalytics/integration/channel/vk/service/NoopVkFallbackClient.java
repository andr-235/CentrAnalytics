package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.client.VkFallbackClient;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;

import java.util.List;

public class NoopVkFallbackClient implements VkFallbackClient {

    @Override
    public List<VkGroupSearchResult> searchGroups(String region, int limit) {
        return List.of();
    }

    @Override
    public List<VkUserSearchResult> searchUsers(String region, int limit) {
        return List.of();
    }

    @Override
    public List<VkWallPostResult> getGroupPosts(Long groupId, int limit) {
        return List.of();
    }

    @Override
    public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
        return List.of();
    }

    @Override
    public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
        return List.of();
    }
}
