package com.ca.centranalytics.integration.api.service;

import com.ca.centranalytics.integration.api.dto.ConversationResponse;
import com.ca.centranalytics.integration.api.dto.ExternalUserResponse;
import com.ca.centranalytics.integration.api.dto.IntegrationSourceResponse;
import com.ca.centranalytics.integration.api.dto.RawEventResponse;
import com.ca.centranalytics.integration.api.exception.IntegrationNotFoundException;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.repository.ConversationRepository;
import com.ca.centranalytics.integration.domain.repository.ExternalUserRepository;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import com.ca.centranalytics.integration.domain.repository.RawEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegrationQueryService {

    private final IntegrationSourceRepository integrationSourceRepository;
    private final ConversationRepository conversationRepository;
    private final ExternalUserRepository externalUserRepository;
    private final RawEventRepository rawEventRepository;

    public List<IntegrationSourceResponse> getIntegrations(Platform platform) {
        return (platform == null ? integrationSourceRepository.findAll() : integrationSourceRepository.findByPlatform(platform)).stream()
                .sorted(Comparator.comparing(source -> source.getId() == null ? Long.MAX_VALUE : source.getId()))
                .map(source -> new IntegrationSourceResponse(
                        source.getId(),
                        source.getPlatform(),
                        source.getName(),
                        source.getStatus()
                ))
                .toList();
    }

    public List<ConversationResponse> getConversations(Platform platform, Long sourceId) {
        return (platform == null ? conversationRepository.findAll() : conversationRepository.findByPlatform(platform)).stream()
                .filter(conversation -> sourceId == null || conversation.getSource().getId().equals(sourceId))
                .map(conversation -> new ConversationResponse(
                        conversation.getId(),
                        conversation.getSource().getId(),
                        conversation.getPlatform(),
                        conversation.getExternalConversationId(),
                        conversation.getType(),
                        conversation.getTitle()
                ))
                .toList();
    }

    public List<ExternalUserResponse> getUsers(Platform platform, String search) {
        String normalizedSearch = search == null ? null : search.toLowerCase();
        return (platform == null ? externalUserRepository.findAll() : externalUserRepository.findByPlatform(platform)).stream()
                .filter(user -> normalizedSearch == null
                        || contains(user.getDisplayName(), normalizedSearch)
                        || contains(user.getUsername(), normalizedSearch)
                        || contains(user.getExternalUserId(), normalizedSearch))
                .map(user -> new ExternalUserResponse(
                        user.getId(),
                        user.getPlatform(),
                        user.getExternalUserId(),
                        user.getDisplayName(),
                        user.getUsername(),
                        user.isBot()
                ))
                .toList();
    }

    public RawEventResponse getRawEvent(Long id) {
        return rawEventRepository.findById(id)
                .map(rawEvent -> new RawEventResponse(
                        rawEvent.getId(),
                        rawEvent.getPlatform(),
                        rawEvent.getEventType(),
                        rawEvent.getEventId(),
                        rawEvent.isSignatureValid(),
                        rawEvent.getProcessingStatus(),
                        rawEvent.getPayloadJson(),
                        rawEvent.getErrorMessage(),
                        rawEvent.getReceivedAt()
                ))
                .orElseThrow(() -> new IntegrationNotFoundException("Raw event not found: " + id));
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }
}
