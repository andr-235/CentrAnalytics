package com.ca.centranalytics.integration.channel.whatsapp.wappi;

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
class WappiWebhookControllerTest {

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
        mockMvc.perform(post("/api/integrations/webhooks/wappi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messages": [
                                    {
                                      "wh_type": "incoming_message",
                                      "profile_id": "6qw54f68-71eq",
                                      "id": "3A945DD9CDD3016BE11B",
                                      "body": "Ответ на сообщение",
                                      "type": "chat",
                                      "from": "79115576366@c.us",
                                      "to": "79602041988@c.us",
                                      "senderName": "Flood",
                                      "chatId": "79115576366@c.us",
                                      "timestamp": "2024-10-21T11:27:57+03:00",
                                      "time": 1729499277,
                                      "caption": null,
                                      "from_where": "phone",
                                      "contact_name": "Максим",
                                      "is_forwarded": false,
                                      "isReply": true,
                                      "is_edited": false,
                                      "stanza_id": "3A9DA038CF9A2CD87E66",
                                      "is_me": false,
                                      "chat_type": "dialog",
                                      "thumbnail": "https://example.com/thumb.jpg",
                                      "picture": "https://example.com/picture.jpg",
                                      "wappi_bot_id": "",
                                      "is_deleted": false,
                                      "is_bot": false,
                                      "reply_message": {
                                        "id": "3A9DA038CF9A2CD87E66",
                                        "body": "https://example.com/reply.jpg",
                                        "type": "image",
                                        "chatId": "",
                                        "timestamp": "2024-10-21T11:21:33+03:00",
                                        "time": 1729498893,
                                        "caption": "Картинка",
                                        "file_name": "reply.jpg",
                                        "contact_name": ""
                                      }
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
    void acceptsIncomingDocumentWebhookAndPersistsAttachmentMetadata() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/wappi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messages": [
                                    {
                                      "wh_type": "incoming_message",
                                      "profile_id": "71ad40e9-b023",
                                      "id": "3EB08F8DF5768B401716",
                                      "body": "https://example.com/1.pdf",
                                      "type": "document",
                                      "from": "79991940200@c.us",
                                      "to": "79662041999@c.us",
                                      "senderName": "Александр Жукин",
                                      "chatId": "79991940200@c.us",
                                      "timestamp": "2023-12-05T13:02:19+03:00",
                                      "time": 1701770539,
                                      "caption": "Это документ",
                                      "from_where": "phone",
                                      "mimetype": "application/pdf",
                                      "contact_name": "Саня Жукин",
                                      "is_forwarded": false,
                                      "isReply": false,
                                      "is_edited": false,
                                      "stanza_id": "",
                                      "is_me": false,
                                      "chat_type": "dialog",
                                      "file_name": "1.pdf",
                                      "thumbnail": null,
                                      "picture": null,
                                      "wappi_bot_id": "",
                                      "is_deleted": false,
                                      "is_bot": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageAttachmentRepository.findAll()).singleElement()
                .satisfies(attachment -> {
                    assertThat(attachment.getAttachmentType()).isEqualTo("document");
                    assertThat(attachment.getUrl()).isEqualTo("https://example.com/1.pdf");
                    assertThat(attachment.getMimeType()).isEqualTo("application/pdf");
                });
    }
}
