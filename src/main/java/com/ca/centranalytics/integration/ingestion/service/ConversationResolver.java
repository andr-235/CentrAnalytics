package com.ca.centranalytics.integration.ingestion.service;

import com.ca.centranalytics.integration.domain.entity.Conversation;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import com.ca.centranalytics.integration.domain.repository.ConversationRepository;
import com.ca.centranalytics.integration.ingestion.dto.InboundConversation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationResolver {

    private final ConversationRepository conversationRepository;

    public Conversation resolve(IntegrationSource source, InboundConversation inboundConversation) {
        return conversationRepository.findBySourceIdAndExternalConversationId(source.getId(), inboundConversation.externalConversationId())
                .map(existing -> {
                    existing.setTitle(inboundConversation.title());
                    existing.setType(inboundConversation.type());
                    existing.setMetadataJson(inboundConversation.metadataJson());
                    return conversationRepository.save(existing);
                })
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .source(source)
                        .platform(source.getPlatform())
                        .externalConversationId(inboundConversation.externalConversationId())
                        .type(inboundConversation.type())
                        .title(inboundConversation.title())
                        .metadataJson(inboundConversation.metadataJson())
                        .build()));
    }
}
