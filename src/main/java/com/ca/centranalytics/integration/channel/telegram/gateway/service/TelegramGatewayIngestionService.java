package com.ca.centranalytics.integration.channel.telegram.gateway.service;

import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramGatewayIngestionService {

    private final IntegrationIngestionService integrationIngestionService;

    public void ingest(InboundIntegrationEvent event) {
        integrationIngestionService.ingest(event);
    }
}
