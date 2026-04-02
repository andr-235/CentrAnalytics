package com.ca.centranalytics.integration.api;

import com.ca.centranalytics.TestcontainersConfiguration;
import com.ca.centranalytics.integration.domain.entity.Conversation;
import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.ExternalUser;
import com.ca.centranalytics.integration.domain.entity.IntegrationSource;
import com.ca.centranalytics.integration.domain.entity.IntegrationStatus;
import com.ca.centranalytics.integration.domain.entity.Message;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.entity.ProcessingStatus;
import com.ca.centranalytics.integration.domain.entity.RawEvent;
import com.ca.centranalytics.integration.domain.repository.ConversationRepository;
import com.ca.centranalytics.integration.domain.repository.ExternalUserRepository;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import com.ca.centranalytics.integration.domain.repository.MessageRepository;
import com.ca.centranalytics.integration.domain.repository.RawEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IntegrationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationSourceRepository integrationSourceRepository;

    @Autowired
    private ExternalUserRepository externalUserRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private RawEventRepository rawEventRepository;

    @Autowired
    private MessageRepository messageRepository;

    private Long messageId;
    private Long rawEventId;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        rawEventRepository.deleteAll();
        conversationRepository.deleteAll();
        externalUserRepository.deleteAll();
        integrationSourceRepository.deleteAll();

        IntegrationSource source = integrationSourceRepository.save(IntegrationSource.builder()
                .platform(Platform.VK)
                .name("VK Demo")
                .status(IntegrationStatus.ACTIVE)
                .settingsJson("{\"groupId\":42}")
                .build());

        ExternalUser author = externalUserRepository.save(ExternalUser.builder()
                .platform(Platform.VK)
                .externalUserId("123")
                .displayName("Ivan Ivanov")
                .username("ivan")
                .metadataJson("{}")
                .build());

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .source(source)
                .platform(Platform.VK)
                .externalConversationId("2000000001")
                .type(ConversationType.GROUP)
                .title("Community chat")
                .metadataJson("{}")
                .build());

        RawEvent rawEvent = rawEventRepository.save(RawEvent.builder()
                .platform(Platform.VK)
                .eventType("message_new")
                .eventId("vk-evt-1")
                .receivedAt(Instant.parse("2026-04-02T00:00:00Z"))
                .payloadJson("{\"type\":\"message_new\"}")
                .signatureValid(true)
                .processingStatus(ProcessingStatus.PERSISTED)
                .build());

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .platform(Platform.VK)
                .externalMessageId("777")
                .author(author)
                .sentAt(Instant.parse("2026-04-02T00:00:00Z"))
                .text("Hello from VK")
                .normalizedText("hello from vk")
                .messageType(MessageType.TEXT)
                .hasAttachments(false)
                .rawEvent(rawEvent)
                .ingestionStatus(ProcessingStatus.PERSISTED)
                .build());

        this.messageId = message.getId();
        this.rawEventId = rawEvent.getId();
    }

    @Test
    void rejectsAnonymousReadRequest() throws Exception {
        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "reader", roles = "USER")
    void returnsReadApiDataForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].platform").value("VK"));

        mockMvc.perform(get("/api/messages").param("search", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalMessageId").value("777"));

        mockMvc.perform(get("/api/messages/{id}", messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationTitle").value("Community chat"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void allowsAdminEndpointsForAdmins() throws Exception {
        mockMvc.perform(get("/api/raw-events/{id}", rawEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("vk-evt-1"));

        mockMvc.perform(post("/api/admin/integrations/vk/register-webhook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("VK"));
    }

    @Test
    @WithMockUser(username = "reader", roles = "USER")
    void forbidsAdminEndpointsForNonAdmins() throws Exception {
        mockMvc.perform(get("/api/raw-events/{id}", rawEventId))
                .andExpect(status().isForbidden());
    }
}
