package com.ca.centranalytics.integration.domain;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class IntegrationPersistenceTest {

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

    @Test
    void persistsCoreIntegrationEntities() {
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
                .metadataJson("{\"city\":\"Moscow\"}")
                .build());

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .source(source)
                .platform(Platform.VK)
                .externalConversationId("2000000001")
                .type(ConversationType.GROUP)
                .title("Community chat")
                .metadataJson("{\"peerId\":2000000001}")
                .build());

        RawEvent rawEvent = rawEventRepository.save(RawEvent.builder()
                .platform(Platform.VK)
                .eventType("message_new")
                .eventId("vk-2000000001-777")
                .payloadJson("{\"type\":\"message_new\"}")
                .signatureValid(true)
                .processingStatus(ProcessingStatus.RECEIVED)
                .receivedAt(Instant.now())
                .build());

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .platform(Platform.VK)
                .externalMessageId("777")
                .author(author)
                .text("Hello from VK")
                .normalizedText("hello from vk")
                .messageType(MessageType.TEXT)
                .hasAttachments(false)
                .rawEvent(rawEvent)
                .ingestionStatus(ProcessingStatus.PERSISTED)
                .sentAt(Instant.now())
                .build());

        assertThat(message.getId()).isNotNull();
        assertThat(messageRepository.findByPlatformAndExternalMessageId(Platform.VK, "777"))
                .isPresent()
                .get()
                .extracting(Message::getConversation)
                .extracting(Conversation::getTitle)
                .isEqualTo("Community chat");
    }
}
