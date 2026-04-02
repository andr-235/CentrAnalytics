package com.ca.centranalytics.integration.ingestion.event;

import com.ca.centranalytics.integration.domain.entity.Message;

public record MessageCapturedEvent(Message message) {
}
