package com.ca.centranalytics.integration.channel.max.wappi;

import com.ca.centranalytics.TestcontainersConfiguration;
import com.ca.centranalytics.integration.domain.repository.MessageAttachmentRepository;
import com.ca.centranalytics.integration.domain.repository.MessageRepository;
import com.ca.centranalytics.integration.domain.repository.RawEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MaxWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;

    @Autowired
    private RawEventRepository rawEventRepository;

    @BeforeEach
    void setUp() {
        messageAttachmentRepository.deleteAll();
        messageRepository.deleteAll();
        rawEventRepository.deleteAll();
    }

    @Test
    void acceptsIncomingMessageWebhookAndPersistsIt() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/wappi/max")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messages": [
                                    {
                                      "wh_type": "incoming_message",
                                      "profile_id": "max-profile-1",
                                      "id": "msg-1",
                                      "body": "Привет из MAX",
                                      "type": "text",
                                      "from": "user-1",
                                      "to": "bot-1",
                                      "senderName": "Sender",
                                      "chatId": "chat-1",
                                      "timestamp": "2026-04-13T10:15:30+03:00",
                                      "time": 1776045330,
                                      "chat_type": "dialog",
                                      "contact_name": "Контакт"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.processed").value(1));

        assertThat(rawEventRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageAttachmentRepository.findAll()).isEmpty();
    }

    @Test
    void ignoresNonIncomingEventsButReturnsProcessedIncomingCount() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/wappi/max")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messages": [
                                    {
                                      "wh_type": "delivery_status",
                                      "profile_id": "max-profile-1",
                                      "id": "status-1",
                                      "type": "text",
                                      "body": "ignored",
                                      "from": "user-1",
                                      "to": "bot-1",
                                      "senderName": "Sender",
                                      "chatId": "chat-1",
                                      "timestamp": "2026-04-13T10:15:30+03:00"
                                    },
                                    {
                                      "wh_type": "incoming_message",
                                      "profile_id": "max-profile-1",
                                      "id": "msg-2",
                                      "body": "Файл",
                                      "type": "document",
                                      "from": "user-2",
                                      "to": "bot-1",
                                      "senderName": "Sender",
                                      "chatId": "chat-2",
                                      "timestamp": "2026-04-13T10:17:30+03:00",
                                      "mimetype": "application/pdf",
                                      "file_name": "report.pdf",
                                      "file_link": "https://files.example/report.pdf"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.processed").value(1));

        assertThat(rawEventRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageAttachmentRepository.findAll()).singleElement()
                .satisfies(attachment -> {
                    assertThat(attachment.getAttachmentType()).isEqualTo("document");
                    assertThat(attachment.getUrl()).isEqualTo("https://files.example/report.pdf");
                    assertThat(attachment.getMimeType()).isEqualTo("application/pdf");
                });
    }

    @Test
    void acceptsIncomingMessageWhenMediaInfoIsAnObject() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/wappi/max")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messages": [
                                    {
                                      "wh_type": "incoming_message",
                                      "profile_id": "max-profile-2",
                                      "id": "msg-3",
                                      "body": "https://files.example/image.jpg",
                                      "type": "image",
                                      "from": "user-3",
                                      "to": "bot-1",
                                      "senderName": "Sender",
                                      "chatId": "chat-3",
                                      "timestamp": "2026-04-13T10:18:30+03:00",
                                      "caption": "Картинка",
                                      "media_info": {
                                        "width": 1024,
                                        "height": 768
                                      },
                                      "mimetype": "image/jpeg",
                                      "file_link": "https://files.example/image.jpg"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.processed").value(1));

        assertThat(rawEventRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageAttachmentRepository.findAll()).singleElement()
                .satisfies(attachment -> {
                    assertThat(attachment.getAttachmentType()).isEqualTo("image");
                    assertThat(attachment.getUrl()).isEqualTo("https://files.example/image.jpg");
                    assertThat(attachment.getMimeType()).isEqualTo("image/jpeg");
                });
    }
}
