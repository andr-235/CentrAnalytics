package com.ca.centranalytics.integration.channel.vk;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "integration.vk.group-id=42",
        "integration.vk.secret=vk-secret",
        "integration.vk.confirmation-code=vk-confirm"
})
class VkWebhookControllerTest {

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
    void returnsConfirmationCode() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/vk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"confirmation","group_id":42}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("vk-confirm"));
    }

    @Test
    void rejectsInvalidSecret() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/vk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"message_new","group_id":42,"secret":"wrong","object":{"message":{"id":1,"peer_id":2,"from_id":3,"date":1775088000,"text":"hi"}}}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("VK secret is invalid"));
    }

    @Test
    void acceptsNewMessageCallbackAndPersistsIt() throws Exception {
        mockMvc.perform(post("/api/integrations/webhooks/vk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"message_new","group_id":42,"secret":"vk-secret","event_id":"vk-evt-1","object":{"message":{"id":777,"peer_id":2000000001,"from_id":123,"date":1775088000,"text":"hello"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        assertThat(rawEventRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).hasSize(1);
    }
}
