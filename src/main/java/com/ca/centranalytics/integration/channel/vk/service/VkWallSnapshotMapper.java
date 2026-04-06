package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import org.springframework.stereotype.Service;

@Service
public class VkWallSnapshotMapper {

    public VkWallPostSnapshot map(VkWallPostResult result, VkCollectionMethod collectionMethod) {
        return VkWallPostSnapshot.builder()
                .ownerId(result.ownerId())
                .postId(result.postId())
                .authorVkUserId(result.authorVkUserId())
                .text(result.text())
                .collectionMethod(collectionMethod)
                .rawJson(result.rawJson())
                .build();
    }
}
