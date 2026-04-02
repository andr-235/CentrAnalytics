package com.ca.centranalytics.integration.channel.telegram;

import com.ca.centranalytics.TestcontainersConfiguration;
import com.ca.centranalytics.integration.domain.repository.MessageRepository;
import com.ca.centranalytics.integration.domain.repository.RawEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = {
        "integration.telegram.webhook-secret=tg-secret"
})
class TelegramWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RawEventRepository rawEventRepository;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        rawEventRepository.deleteAll();
    }

    @Test
    void rejectsInvalidSecret() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Telegram-Bot-Api-Secret-Token", "wrong")
                        .content("""
                                {"update_id":1,"message":{"message_id":1,"date":1775088000,"text":"hello","chat":{"id":100,"type":"group","title":"TG"},"from":{"id":55,"is_bot":false,"first_name":"Petr"}}}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Telegram webhook secret is invalid"));
    }

    @Test
    void acceptsValidUpdateAndPersistsIt() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Telegram-Bot-Api-Secret-Token", "tg-secret")
                        .content("""
                                {"update_id":1,"message":{"message_id":501,"date":1775088000,"text":"hello","chat":{"id":100,"type":"group","title":"TG"},"from":{"id":55,"is_bot":false,"first_name":"Petr","username":"petya"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        assertThat(rawEventRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).hasSize(1);
    }
}
