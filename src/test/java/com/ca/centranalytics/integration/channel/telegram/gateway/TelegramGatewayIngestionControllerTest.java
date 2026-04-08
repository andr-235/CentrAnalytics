package com.ca.centranalytics.integration.channel.telegram.gateway;

import com.ca.centranalytics.integration.channel.telegram.gateway.config.TelegramGatewayIngestionProperties;
import com.ca.centranalytics.integration.channel.telegram.gateway.controller.TelegramGatewayIngestionController;
import com.ca.centranalytics.integration.channel.telegram.gateway.service.TelegramGatewayIngestionService;
import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.ingestion.dto.InboundAuthor;
import com.ca.centranalytics.integration.ingestion.dto.InboundConversation;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.dto.InboundMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramGatewayIngestionControllerTest {

    @Test
    void acceptsTrustedTelegramEventAndDelegatesToIngestionService() {
        RecordingTelegramGatewayIngestionService service = new RecordingTelegramGatewayIngestionService();
        TelegramGatewayIngestionController controller = new TelegramGatewayIngestionController(
                new TelegramGatewayIngestionProperties(true, "test-internal-token"),
                service
        );

        Map<String, Object> response = controller.ingest("test-internal-token", validEvent());

        assertThat(response).containsEntry("status", "accepted");
        assertThat(service.recordedEvent).isEqualTo(validEvent());
    }

    @Test
    void rejectsRequestWithInvalidInternalToken() {
        RecordingTelegramGatewayIngestionService service = new RecordingTelegramGatewayIngestionService();
        TelegramGatewayIngestionController controller = new TelegramGatewayIngestionController(
                new TelegramGatewayIngestionProperties(true, "test-internal-token"),
                service
        );

        assertThatThrownBy(() -> controller.ingest("wrong-token", validEvent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid internal token");

        assertThat(service.recordedEvent).isNull();
    }

    private InboundIntegrationEvent validEvent() {
        return new InboundIntegrationEvent(
                Platform.TELEGRAM,
                "message_new",
                "telegram-user-message_new-100-200",
                "{\"chatId\":100,\"messageId\":200}",
                true,
                "700000001",
                "Telegram user +79990000001",
                "{\"mode\":\"user\",\"sessionId\":\"gateway\",\"phoneNumber\":\"+79990000001\"}",
                new InboundConversation("100", ConversationType.GROUP, "Regional Group", "{\"chatType\":\"Channel\"}"),
                new InboundAuthor("2001", "Ivan Ivanov", "ivan", "Ivan", "Ivanov", null, null, false, "{\"source\":\"user\"}"),
                new InboundMessage(
                        "200",
                        Instant.parse("2026-04-08T00:00:00Z"),
                        "Hello from Telegram",
                        "hello from telegram",
                        MessageType.TEXT,
                        null,
                        null,
                        List.of()
                )
        );
    }

    private static final class RecordingTelegramGatewayIngestionService extends TelegramGatewayIngestionService {

        private InboundIntegrationEvent recordedEvent;

        private RecordingTelegramGatewayIngestionService() {
            super(null);
        }

        @Override
        public void ingest(InboundIntegrationEvent event) {
            this.recordedEvent = event;
        }
    }
}
