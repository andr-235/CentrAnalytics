package com.ca.centranalytics.integration.channel.telegram;

import com.ca.centranalytics.integration.channel.telegram.service.TelegramInboundEventMapper;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramInboundEventMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TelegramInboundEventMapper mapper = new TelegramInboundEventMapper(objectMapper);

    @Test
    void mapsTelegramMessageUpdateIntoInboundEvent() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "update_id": 1,
                  "message": {
                    "message_id": 501,
                    "date": 1775088000,
                    "text": "Hello Telegram",
                    "chat": {
                      "id": -1001234567890,
                      "type": "supergroup",
                      "title": "Demo TG"
                    },
                    "from": {
                      "id": 55,
                      "is_bot": false,
                      "first_name": "Petr",
                      "last_name": "Petrov",
                      "username": "petya"
                    }
                  }
                }
                """);

        var event = mapper.map(payload);

        assertThat(event.platform()).isEqualTo(Platform.TELEGRAM);
        assertThat(event.conversation().title()).isEqualTo("Demo TG");
        assertThat(event.author().externalUserId()).isEqualTo("55");
        assertThat(event.message().externalMessageId()).isEqualTo("501");
        assertThat(event.message().text()).isEqualTo("Hello Telegram");
    }
}
