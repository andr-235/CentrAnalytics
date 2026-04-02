package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.dto.VkCallbackRequest;
import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.ingestion.dto.InboundAttachment;
import com.ca.centranalytics.integration.ingestion.dto.InboundAuthor;
import com.ca.centranalytics.integration.ingestion.dto.InboundConversation;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.dto.InboundMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VkInboundEventMapper {

    private final ObjectMapper objectMapper;

    public InboundIntegrationEvent map(VkCallbackRequest request) {
        JsonNode messageNode = request.object() == null ? null : request.object().path("message");
        if (messageNode == null || messageNode.isMissingNode()) {
            throw new IllegalArgumentException("VK callback message payload is missing");
        }

        String peerId = messageNode.path("peer_id").asText();
        String messageId = messageNode.path("id").asText();
        String text = messageNode.path("text").asText(null);
        JsonNode fromIdNode = messageNode.path("from_id");

        return new InboundIntegrationEvent(
                Platform.VK,
                request.type(),
                StringUtils.hasText(request.eventId()) ? request.eventId() : "vk-" + peerId + "-" + messageId,
                writeValue(request),
                true,
                String.valueOf(request.groupId()),
                "VK group " + request.groupId(),
                "{\"groupId\":" + request.groupId() + "}",
                new InboundConversation(
                        peerId,
                        ConversationType.GROUP,
                        request.object().path("group_name").asText("VK conversation " + peerId),
                        "{\"peerId\":" + peerId + "}"
                ),
                new InboundAuthor(
                        fromIdNode.asText(),
                        "VK user " + fromIdNode.asText(),
                        null,
                        null,
                        null,
                        null,
                        "https://vk.com/id" + fromIdNode.asText(),
                        false,
                        "{}"
                ),
                new InboundMessage(
                        messageId,
                        Instant.ofEpochSecond(messageNode.path("date").asLong()),
                        text,
                        text == null ? null : text.toLowerCase(),
                        MessageType.TEXT,
                        replyId(messageNode),
                        null,
                        extractAttachments(messageNode.path("attachments"))
                )
        );
    }

    private List<InboundAttachment> extractAttachments(JsonNode attachmentsNode) {
        if (!attachmentsNode.isArray()) {
            return List.of();
        }

        List<InboundAttachment> attachments = new ArrayList<>();
        attachmentsNode.forEach(node -> attachments.add(new InboundAttachment(
                node.path("type").asText("unknown"),
                null,
                null,
                null,
                "{}"
        )));
        return attachments;
    }

    private String replyId(JsonNode messageNode) {
        JsonNode replyNode = messageNode.path("reply_message");
        return replyNode.isMissingNode() ? null : replyNode.path("id").asText(null);
    }

    private String writeValue(VkCallbackRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize VK callback payload", e);
        }
    }
}
