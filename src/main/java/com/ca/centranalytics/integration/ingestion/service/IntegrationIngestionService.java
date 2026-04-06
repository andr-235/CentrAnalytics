package com.ca.centranalytics.integration.ingestion.service;

import com.ca.centranalytics.integration.domain.entity.Conversation;
import com.ca.centranalytics.integration.domain.entity.ExternalUser;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import com.ca.centranalytics.integration.domain.entity.IntegrationStatus;
import com.ca.centranalytics.integration.domain.entity.Message;
import com.ca.centranalytics.integration.domain.entity.ProcessingStatus;
import com.ca.centranalytics.integration.domain.entity.RawEvent;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.event.MessageCapturedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class IntegrationIngestionService {

    private final RawEventService rawEventService;
    private final IntegrationSourceRepository integrationSourceRepository;
    private final ConversationResolver conversationResolver;
    private final ExternalUserResolver externalUserResolver;
    private final MessagePersistenceService messagePersistenceService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void ingest(InboundIntegrationEvent event) {
        validateEvent(event);

        if (rawEventService.findExisting(event.platform(), event.eventId()).isPresent()) {
            return;
        }

        RawEvent rawEvent = rawEventService.createReceivedEvent(event);
        try {
            IntegrationSource source = resolveSource(event);
            rawEventService.updateStatus(rawEvent, ProcessingStatus.NORMALIZED, null);

            Conversation conversation = conversationResolver.resolve(source, event.conversation());
            ExternalUser author = externalUserResolver.resolve(source, event.author());
            Message message = messagePersistenceService.persist(conversation, author, rawEvent, event.message());

            rawEventService.updateStatus(rawEvent, ProcessingStatus.PERSISTED, null);
            applicationEventPublisher.publishEvent(new MessageCapturedEvent(message));
        } catch (RuntimeException ex) {
            rawEventService.updateStatus(rawEvent, ProcessingStatus.FAILED, ex.getMessage());
            throw ex;
        }
    }

    private IntegrationSource resolveSource(InboundIntegrationEvent event) {
        if (!StringUtils.hasText(event.sourceExternalId())) {
            throw new IllegalArgumentException("Inbound event sourceExternalId is required");
        }

        return integrationSourceRepository.findByPlatformAndSourceExternalId(event.platform(), event.sourceExternalId())
                .or(() -> integrationSourceRepository.findByPlatform(event.platform()).stream()
                        .filter(source -> !StringUtils.hasText(source.getSourceExternalId()))
                        .filter(source -> StringUtils.hasText(source.getSettingsJson()) && source.getSettingsJson().equals(event.sourceSettingsJson()))
                        .findFirst())
                .map(existing -> {
                    existing.setSourceExternalId(event.sourceExternalId());
                    existing.setName(event.sourceName());
                    existing.setStatus(IntegrationStatus.ACTIVE);
                    existing.setSettingsJson(event.sourceSettingsJson());
                    return integrationSourceRepository.save(existing);
                })
                .orElseGet(() -> integrationSourceRepository.save(IntegrationSource.builder()
                        .platform(event.platform())
                        .sourceExternalId(event.sourceExternalId())
                        .name(event.sourceName())
                        .status(IntegrationStatus.ACTIVE)
                        .settingsJson(event.sourceSettingsJson())
                        .build()));
    }

    private void validateEvent(InboundIntegrationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Inbound event is required");
        }
        if (event.platform() == null) {
            throw new IllegalArgumentException("Inbound event platform is required");
        }
        if (!StringUtils.hasText(event.eventId())) {
            throw new IllegalArgumentException("Inbound event eventId is required");
        }
        if (event.conversation() == null) {
            throw new IllegalArgumentException("Inbound event conversation is required");
        }
        if (event.message() == null) {
            throw new IllegalArgumentException("Inbound event message is required");
        }
    }
}
