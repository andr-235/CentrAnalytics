package com.ca.centranalytics.integration.channel.max.wappi;

import com.ca.centranalytics.integration.channel.max.wappi.client.dto.MaxContactDto;
import com.ca.centranalytics.integration.channel.max.wappi.client.dto.MaxLocationDto;
import com.ca.centranalytics.integration.channel.max.wappi.client.dto.MaxMessageDto;
import com.ca.centranalytics.integration.channel.max.wappi.service.MaxInboundEventMapper;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxInboundEventMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MaxInboundEventMapper mapper = new MaxInboundEventMapper(objectMapper);

    @Test
    void mapsTextIncomingMessageIntoInboundEvent() {
        MaxMessageDto message = new MaxMessageDto(
                "incoming_message",
                "max-profile-1",
                "msg-1",
                "Привет из MAX",
                "text",
                "user-1",
                "bot-1",
                "Sender",
                "chat-1",
                "2026-04-13T10:15:30+03:00",
                1776045330L,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                "dialog",
                null,
                null,
                null,
                null,
                null,
                null
        );

        var event = mapper.map(message);

        assertThat(event.platform()).isEqualTo(Platform.MAX);
        assertThat(event.eventType()).isEqualTo("incoming_message");
        assertThat(event.eventId()).isEqualTo("max-max-profile-1-msg-1");
        assertThat(event.sourceExternalId()).isEqualTo("max-profile-1");
        assertThat(event.conversation().externalConversationId()).isEqualTo("chat-1");
        assertThat(event.author().externalUserId()).isEqualTo("user-1");
        assertThat(event.author().displayName()).isEqualTo("Sender");
        assertThat(event.message().externalMessageId()).isEqualTo("msg-1");
        assertThat(event.message().messageType()).isEqualTo(MessageType.TEXT);
        assertThat(event.message().text()).isEqualTo("Привет из MAX");
        assertThat(event.message().attachments()).isEmpty();
    }

    @Test
    void mapsDocumentIncomingMessageWithAttachmentMetadata() {
        MaxMessageDto message = new MaxMessageDto(
                "incoming_message",
                "max-profile-1",
                "msg-2",
                "https://files.example/test.pdf",
                "document",
                "user-2",
                "bot-1",
                "Sender",
                "chat-2",
                null,
                1776045330L,
                "Это документ",
                "application/pdf",
                null,
                "test.pdf",
                "https://files.example/test.pdf",
                false,
                false,
                false,
                "dialog",
                null,
                null,
                null,
                null,
                null,
                null
        );

        var event = mapper.map(message);

        assertThat(event.message().messageType()).isEqualTo(MessageType.DOCUMENT);
        assertThat(event.message().text()).isEqualTo("Это документ");
        assertThat(event.message().attachments()).singleElement()
                .satisfies(attachment -> {
                    assertThat(attachment.attachmentType()).isEqualTo("document");
                    assertThat(attachment.externalAttachmentId()).isEqualTo("msg-2");
                    assertThat(attachment.url()).isEqualTo("https://files.example/test.pdf");
                    assertThat(attachment.mimeType()).isEqualTo("application/pdf");
                    assertThat(attachment.fileName()).isEqualTo("test.pdf");
                    assertThat(attachment.contentBase64()).isNull();
                });
    }

    @Test
    void mapsLocationMessageAsTextWithoutAttachments() {
        MaxMessageDto message = new MaxMessageDto(
                "incoming_message",
                "max-profile-1",
                "msg-3",
                null,
                "location",
                "user-3",
                "bot-1",
                "Geo Sender",
                "chat-3",
                "2026-04-13T10:15:30+03:00",
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                "dialog",
                new MaxLocationDto(43.1155, 131.8855),
                null,
                null,
                null,
                null,
                null
        );

        var event = mapper.map(message);

        assertThat(event.message().messageType()).isEqualTo(MessageType.TEXT);
        assertThat(event.message().text()).contains("43.1155").contains("131.8855");
        assertThat(event.message().attachments()).isEmpty();
    }

    @Test
    void prefersContactNameForDisplayName() {
        MaxMessageDto message = new MaxMessageDto(
                "incoming_message",
                "max-profile-1",
                "msg-4",
                "Контакт",
                "text",
                "user-4",
                "bot-1",
                "Sender",
                "chat-4",
                "2026-04-13T10:15:30+03:00",
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                "dialog",
                null,
                new MaxContactDto("c-1", "Контактное имя", "+79990000000"),
                "Контактное имя",
                null,
                null,
                null
        );

        var event = mapper.map(message);

        assertThat(event.author().displayName()).isEqualTo("Контактное имя");
        assertThat(event.author().phone()).isEqualTo("+79990000000");
    }
}
