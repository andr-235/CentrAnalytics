package com.ca.centranalytics.integration.channel.vk.client;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkRegionalCity;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;

import java.util.List;

public interface VkOfficialClient {
    List<String> resolveRegionalSearchTerms(String region);
    List<VkRegionalCity> resolveRegionalCities(String region);
    List<VkGroupSearchResult> searchGroups(String region, int limit);
    List<VkUserSearchResult> searchUsers(String region, int limit);
    List<VkUserSearchResult> searchUsers(VkRegionalCity city, int limit);
    List<VkWallPostResult> getGroupPosts(String domain, int limit);
    List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit);
    List<VkUserSearchResult> getUsersByIds(List<Long> userIds);
}
