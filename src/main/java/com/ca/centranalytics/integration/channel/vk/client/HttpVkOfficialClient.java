package com.ca.centranalytics.integration.channel.vk.client;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HttpVkOfficialClient implements VkOfficialClient {

    private final VkProperties vkProperties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public List<VkGroupSearchResult> searchGroups(String region, int limit) {
        throw notImplemented("groups.search");
    }

    @Override
    public List<VkUserSearchResult> searchUsers(String region, int limit) {
        throw notImplemented("users.search");
    }

    @Override
    public List<VkWallPostResult> getGroupPosts(Long groupId, int limit) {
        throw notImplemented("wall.get");
    }

    @Override
    public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
        throw notImplemented("wall.getComments");
    }

    @Override
    public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
        throw notImplemented("users.get");
    }

    private UnsupportedOperationException notImplemented(String method) {
        RestClient restClient = restClientBuilder.baseUrl(vkProperties.apiBaseUrl()).build();
        return new UnsupportedOperationException(
                "VK official client method is not implemented yet: " + method + " via " + restClient
        );
    }
}
