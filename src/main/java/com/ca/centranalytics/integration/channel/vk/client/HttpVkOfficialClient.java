package com.ca.centranalytics.integration.channel.vk.client;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HttpVkOfficialClient implements VkOfficialClient {

    private final VkProperties vkProperties;

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
