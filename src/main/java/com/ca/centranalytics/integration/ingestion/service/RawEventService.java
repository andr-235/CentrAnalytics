package com.ca.centranalytics.integration.ingestion.service;

import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.entity.ProcessingStatus;
import com.ca.centranalytics.integration.domain.entity.RawEvent;
import com.ca.centranalytics.integration.domain.repository.RawEventRepository;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RawEventService {

    private final RawEventRepository rawEventRepository;

    public Optional<RawEvent> findExisting(Platform platform, String eventId) {
        return rawEventRepository.findByPlatformAndEventId(platform, eventId);
    }

    public RawEvent createReceivedEvent(InboundIntegrationEvent event) {
        return rawEventRepository.save(RawEvent.builder()
                .platform(event.platform())
                .eventType(event.eventType())
                .eventId(event.eventId())
                .receivedAt(Instant.now())
                .payloadJson(event.rawPayload())
                .signatureValid(event.signatureValid())
                .processingStatus(ProcessingStatus.RECEIVED)
                .build());
    }

    public RawEvent updateStatus(RawEvent rawEvent, ProcessingStatus status, String errorMessage) {
        rawEvent.setProcessingStatus(status);
        rawEvent.setErrorMessage(errorMessage);
        return rawEventRepository.save(rawEvent);
    }
}
