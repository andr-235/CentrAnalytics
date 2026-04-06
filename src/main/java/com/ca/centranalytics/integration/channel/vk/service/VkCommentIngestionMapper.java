package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.ingestion.dto.InboundConversation;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.dto.InboundMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VkCommentIngestionMapper {

    private final VkAuthorNormalizationService vkAuthorNormalizationService;

    public InboundIntegrationEvent map(VkGroupCandidate group, VkUserCandidate author, VkCommentSnapshot snapshot) {
        String ownerId = String.valueOf(snapshot.getOwnerId());
        String postId = String.valueOf(snapshot.getPostId());
        String commentId = String.valueOf(snapshot.getCommentId());

        return new InboundIntegrationEvent(
                Platform.VK,
                "wall_comment",
                "vk-comment-" + ownerId + "-" + postId + "-" + commentId,
                snapshot.getRawJson(),
                true,
                String.valueOf(group.getVkGroupId()),
                group.getName(),
                group.getRawJson(),
                new InboundConversation(
                        "wall-" + ownerId + "-" + postId,
                        ConversationType.THREAD,
                        group.getName() + " comments",
                        group.getRawJson()
                ),
                vkAuthorNormalizationService.toInboundAuthor(author),
                new InboundMessage(
                        commentId,
                        Instant.now(),
                        snapshot.getText(),
                        snapshot.getText() == null ? null : snapshot.getText().toLowerCase(),
                        MessageType.TEXT,
                        postId,
                        null,
                        List.of()
                )
        );
    }
}
