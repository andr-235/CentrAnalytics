package com.ca.centranalytics.integration.ingestion;

import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.ingestion.dto.InboundAttachment;
import com.ca.centranalytics.integration.ingestion.dto.InboundAuthor;
import com.ca.centranalytics.integration.ingestion.dto.InboundConversation;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.dto.InboundMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InboundIntegrationEventTest {

    @Test
    void exposesAllFieldsNeededForPersistence() {
        InboundIntegrationEvent event = new InboundIntegrationEvent(
                Platform.TELEGRAM,
                "message",
                "telegram-100-55",
                "{\"update_id\":1}",
                true,
                "telegram-demo",
                "Demo source",
                "{\"bot\":\"centranalytics_bot\"}",
                new InboundConversation("100", ConversationType.GROUP, "Demo chat", "{\"chatId\":100}"),
                new InboundAuthor("55", "Petya", "petya", "Petya", "Petrov", null, null, false, "{\"lang\":\"ru\"}"),
                new InboundMessage(
                        "501",
                        Instant.parse("2026-04-02T00:00:00Z"),
                        "Hello",
                        "hello",
                        MessageType.TEXT,
                        "500",
                        null,
                        List.of(new InboundAttachment("photo", "ph-1", "https://example.com/1.jpg", "image/jpeg", "{}"))
                )
        );

        assertThat(event.platform()).isEqualTo(Platform.TELEGRAM);
        assertThat(event.conversation().externalConversationId()).isEqualTo("100");
        assertThat(event.author().externalUserId()).isEqualTo("55");
        assertThat(event.message().attachments()).hasSize(1);
        assertThat(event.message().attachments().getFirst().attachmentType()).isEqualTo("photo");
    }
}
