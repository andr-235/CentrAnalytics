package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.dto.VkCallbackRequest;
import com.ca.centranalytics.integration.channel.vk.service.VkInboundEventMapper;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VkInboundEventMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VkInboundEventMapper mapper = new VkInboundEventMapper(objectMapper);

    @Test
    void mapsMessageNewCallbackIntoInboundEvent() throws Exception {
        VkCallbackRequest request = objectMapper.readValue("""
                {
                  "type": "message_new",
                  "secret": "vk-secret",
                  "group_id": 42,
                  "event_id": "vk-evt-1",
                  "object": {
                    "message": {
                      "id": 777,
                      "peer_id": 2000000001,
                      "from_id": 123,
                      "date": 1775088000,
                      "text": "Hello VK",
                      "attachments": [{"type": "photo"}]
                    }
                  }
                }
                """, VkCallbackRequest.class);

        var event = mapper.map(request);

        assertThat(event.platform()).isEqualTo(Platform.VK);
        assertThat(event.eventId()).isEqualTo("vk-evt-1");
        assertThat(event.conversation().externalConversationId()).isEqualTo("2000000001");
        assertThat(event.author().externalUserId()).isEqualTo("123");
        assertThat(event.message().externalMessageId()).isEqualTo("777");
        assertThat(event.message().attachments()).hasSize(1);
    }
}
