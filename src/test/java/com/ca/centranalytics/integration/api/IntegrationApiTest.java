package com.ca.centranalytics.integration.api;

import com.ca.centranalytics.TestcontainersConfiguration;
import com.ca.centranalytics.integration.channel.vk.client.VkFallbackClient;
import com.ca.centranalytics.integration.channel.vk.client.VkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import com.ca.centranalytics.integration.channel.vk.repository.VkCrawlJobRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, IntegrationApiTest.TestConfig.class})
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

    @Autowired
    private VkCrawlJobRepository vkCrawlJobRepository;

    private Long messageId;
    private Long rawEventId;
    private Long vkCrawlJobId;

    @BeforeEach
    void setUp() {
        vkCrawlJobRepository.deleteAll();
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
                .source(source)
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
        this.vkCrawlJobId = vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.GROUP_SEARCH)
                .status(VkCrawlJobStatus.RUNNING)
                .requestJson("{\"region\":\"Primorsky Krai\",\"limit\":25}")
                .itemCount(25)
                .processedCount(10)
                .warningCount(1)
                .build()).getId();
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
                .andExpect(jsonPath("$[0].platform").value("VK"))
                .andExpect(jsonPath("$[0].settingsJson").doesNotExist());

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

        mockMvc.perform(post("/api/admin/integrations/vk/groups/search")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"region":"Primorsky Krai","limit":25,"collectionMode":"HYBRID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobType").value("GROUP_SEARCH"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.id").isNumber());

        mockMvc.perform(post("/api/admin/integrations/vk/groups/{groupId}/posts/collect", 1001L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"limit":25,"collectionMode":"HYBRID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobType").value("GROUP_POSTS"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/admin/integrations/vk/posts/comments/collect")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"postIds":[3003],"limit":25,"collectionMode":"HYBRID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobType").value("POST_COMMENTS"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/admin/integrations/vk/users/enrich")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"userIds":[2002],"collectionMode":"HYBRID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobType").value("AUTHOR_PROFILE_ENRICH"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/admin/integrations/vk/jobs/{jobId}", vkCrawlJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vkCrawlJobId))
                .andExpect(jsonPath("$.jobType").value("GROUP_SEARCH"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.processedCount").value(10))
                .andExpect(jsonPath("$.warningCount").value(1));
    }

    @Test
    @WithMockUser(username = "reader", roles = "USER")
    void forbidsAdminEndpointsForNonAdmins() throws Exception {
        mockMvc.perform(get("/api/raw-events/{id}", rawEventId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void validatesVkGroupSearchPayload() throws Exception {
        mockMvc.perform(post("/api/admin/integrations/vk/groups/search")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"region":"","limit":0,"collectionMode":"HYBRID"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.region").exists())
                .andExpect(jsonPath("$.limit").exists());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        VkOfficialClient vkOfficialClient() {
            return new VkOfficialClient() {
                @Override
                public List<VkGroupSearchResult> searchGroups(String region, int limit) {
                    return List.of(new VkGroupSearchResult(
                            1001L,
                            "Primorye Group",
                            "primorye_group",
                            "News from Primorsky Krai",
                            "Vladivostok",
                            "{\"id\":1001}"
                    ));
                }

                @Override
                public List<VkUserSearchResult> searchUsers(String region, int limit) {
                    return List.of(new VkUserSearchResult(
                            2002L,
                            "Ivan Ivanov",
                            "Ivan",
                            "Ivanov",
                            "https://vk.com/id2002",
                            "Primorsky Krai",
                            "{\"id\":2002}"
                    ));
                }

                @Override
                public List<VkWallPostResult> getGroupPosts(Long groupId, int limit) {
                    return List.of(new VkWallPostResult(
                            -groupId,
                            3003L,
                            2002L,
                            "Hello from Primorye",
                            Instant.parse("2026-04-06T00:00:00Z"),
                            "{\"owner_id\":-" + groupId + ",\"id\":3003}"
                    ));
                }

                @Override
                public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
                    return List.of(new VkCommentResult(
                            ownerId,
                            postId,
                            4004L,
                            2002L,
                            "Great post",
                            Instant.parse("2026-04-06T00:05:00Z"),
                            "{\"owner_id\":" + ownerId + ",\"post_id\":" + postId + ",\"id\":4004}"
                    ));
                }

                @Override
                public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
                    return userIds.stream()
                            .map(userId -> new VkUserSearchResult(
                                    userId,
                                    userId.equals(2002L) ? "Ivan Ivanov" : "Petr Petrov",
                                    userId.equals(2002L) ? "Ivan" : "Petr",
                                    userId.equals(2002L) ? "Ivanov" : "Petrov",
                                    "https://vk.com/id" + userId,
                                    "Primorsky Krai",
                                    "{\"id\":" + userId + "}"
                            ))
                            .toList();
                }
            };
        }

        @Bean
        @Primary
        VkFallbackClient vkFallbackClient() {
            return new VkFallbackClient() {
                @Override
                public List<VkGroupSearchResult> searchGroups(String region, int limit) {
                    return List.of();
                }

                @Override
                public List<VkUserSearchResult> searchUsers(String region, int limit) {
                    return List.of();
                }

                @Override
                public List<VkWallPostResult> getGroupPosts(Long groupId, int limit) {
                    return List.of();
                }

                @Override
                public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
                    return List.of();
                }

                @Override
                public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
                    return List.of();
                }
            };
        }
    }
}
