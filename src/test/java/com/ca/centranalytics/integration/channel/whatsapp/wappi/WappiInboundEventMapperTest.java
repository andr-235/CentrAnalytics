package com.ca.centranalytics.integration.channel.whatsapp.wappi;

import com.ca.centranalytics.integration.channel.whatsapp.wappi.client.dto.WappiMessageDto;
import com.ca.centranalytics.integration.channel.whatsapp.wappi.client.dto.WappiReplyMessageDto;
import com.ca.centranalytics.integration.channel.whatsapp.wappi.service.WappiInboundEventMapper;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WappiInboundEventMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WappiInboundEventMapper mapper = new WappiInboundEventMapper(objectMapper);

    @Test
    void mapsTextIncomingMessageIntoInboundEvent() {
        WappiMessageDto message = new WappiMessageDto(
                "incoming_message",
                "6qw54f68-71eq",
                "3A945DD9CDD3016BE11B",
                "Ответ на сообщение",
                "chat",
                "79115576366@c.us",
                "79602041988@c.us",
                "Flood",
                "79115576366@c.us",
                "2024-10-21T11:27:57+03:00",
                1729499277L,
                null,
                "phone",
                null,
                "Максим",
                false,
                true,
                false,
                "3A9DA038CF9A2CD87E66",
                false,
                "dialog",
                null,
                null,
                new WappiReplyMessageDto(
                        "3A9DA038CF9A2CD87E66",
                        "Предыдущее сообщение",
                        "chat",
                        "",
                        "2024-10-21T11:21:33+03:00",
                        1729498893L,
                        null,
                        null,
                        null
                ),
                "https://example.com/thumb.jpg",
                "https://example.com/picture.jpg",
                "",
                false,
                false
        );

        var event = mapper.map(message);

        assertThat(event.platform()).isEqualTo(Platform.WAPPI);
        assertThat(event.eventType()).isEqualTo("incoming_message");
        assertThat(event.eventId()).isEqualTo("wappi-6qw54f68-71eq-3A945DD9CDD3016BE11B");
        assertThat(event.sourceExternalId()).isEqualTo("6qw54f68-71eq");
        assertThat(event.conversation().externalConversationId()).isEqualTo("79115576366@c.us");
        assertThat(event.author().externalUserId()).isEqualTo("79115576366@c.us");
        assertThat(event.author().displayName()).isEqualTo("Максим");
        assertThat(event.message().externalMessageId()).isEqualTo("3A945DD9CDD3016BE11B");
        assertThat(event.message().messageType()).isEqualTo(MessageType.TEXT);
        assertThat(event.message().text()).isEqualTo("Ответ на сообщение");
        assertThat(event.message().replyToExternalMessageId()).isEqualTo("3A9DA038CF9A2CD87E66");
        assertThat(event.message().attachments()).isEmpty();
    }

    @Test
    void mapsDocumentIncomingMessageWithBinaryPayload() {
        WappiMessageDto message = new WappiMessageDto(
                "incoming_message",
                "71ad40e9-b023",
                "3EB08F8DF5768B401716",
                "JVBERi0xLjMJUVPRgo=",
                "document",
                "79991940200@c.us",
                "79662041999@c.us",
                "Александр Жукин",
                "79991940200@c.us",
                "2023-12-05T13:02:19+03:00",
                1701770539L,
                "Это документ",
                "phone",
                "application/pdf",
                "Саня Жукин",
                false,
                false,
                false,
                "",
                false,
                "dialog",
                "1.pdf",
                "Это документ",
                null,
                null,
                null,
                "",
                false,
                false
        );

        var event = mapper.map(message);

        assertThat(event.message().messageType()).isEqualTo(MessageType.DOCUMENT);
        assertThat(event.message().text()).isEqualTo("Это документ");
        assertThat(event.message().attachments()).singleElement()
                .satisfies(attachment -> {
                    assertThat(attachment.attachmentType()).isEqualTo("document");
                    assertThat(attachment.externalAttachmentId()).isEqualTo("3EB08F8DF5768B401716");
                    assertThat(attachment.mimeType()).isEqualTo("application/pdf");
                    assertThat(attachment.fileName()).isEqualTo("1.pdf");
                    assertThat(attachment.contentBase64()).isEqualTo("JVBERi0xLjMJUVPRgo=");
                });
    }
}
