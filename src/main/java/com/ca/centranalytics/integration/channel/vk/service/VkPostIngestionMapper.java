package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
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
public class VkPostIngestionMapper {

    private final VkAuthorNormalizationService vkAuthorNormalizationService;

    public InboundIntegrationEvent map(VkGroupCandidate group, VkUserCandidate author, VkWallPostSnapshot snapshot) {
        String ownerId = String.valueOf(snapshot.getOwnerId());
        String postId = String.valueOf(snapshot.getPostId());

        return new InboundIntegrationEvent(
                Platform.VK,
                "wall_post",
                "vk-wall-" + ownerId + "-" + postId,
                snapshot.getRawJson(),
                true,
                String.valueOf(group.getVkGroupId()),
                group.getName(),
                group.getRawJson(),
                new InboundConversation(
                        "wall-" + ownerId,
                        ConversationType.GROUP,
                        group.getName(),
                        group.getRawJson()
                ),
                vkAuthorNormalizationService.toInboundAuthor(author),
                new InboundMessage(
                        postId,
                        Instant.now(),
                        snapshot.getText(),
                        snapshot.getText() == null ? null : snapshot.getText().toLowerCase(),
                        MessageType.TEXT,
                        null,
                        null,
                        List.of()
                )
        );
    }
}
