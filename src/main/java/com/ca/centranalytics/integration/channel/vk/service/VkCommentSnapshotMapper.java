package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import org.springframework.stereotype.Service;

@Service
public class VkCommentSnapshotMapper {

    public VkCommentSnapshot map(VkCommentResult result, VkCollectionMethod collectionMethod) {
        return VkCommentSnapshot.builder()
                .ownerId(result.ownerId())
                .postId(result.postId())
                .commentId(result.commentId())
                .authorVkUserId(result.authorVkUserId())
                .text(result.text())
                .collectionMethod(collectionMethod)
                .rawJson(result.rawJson())
                .build();
    }
}
