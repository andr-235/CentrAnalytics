package com.ca.centranalytics.integration.ingestion;

import com.ca.centranalytics.TestcontainersConfiguration;
import com.ca.centranalytics.integration.domain.entity.ConversationType;
import com.ca.centranalytics.integration.domain.entity.IntegrationStatus;
import com.ca.centranalytics.integration.domain.entity.MessageType;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.entity.ProcessingStatus;
import com.ca.centranalytics.integration.domain.repository.ConversationRepository;
import com.ca.centranalytics.integration.domain.repository.ExternalUserRepository;
import com.ca.centranalytics.integration.domain.repository.IntegrationSourceRepository;
import com.ca.centranalytics.integration.domain.repository.MessageAttachmentRepository;
import com.ca.centranalytics.integration.domain.repository.MessageRepository;
import com.ca.centranalytics.integration.domain.repository.RawEventRepository;
import com.ca.centranalytics.integration.ingestion.dto.InboundAttachment;
import com.ca.centranalytics.integration.ingestion.dto.InboundAuthor;
import com.ca.centranalytics.integration.ingestion.dto.InboundConversation;
import com.ca.centranalytics.integration.ingestion.dto.InboundIntegrationEvent;
import com.ca.centranalytics.integration.ingestion.dto.InboundMessage;
import com.ca.centranalytics.integration.ingestion.service.IntegrationIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class IntegrationIngestionServiceTest {

    @Autowired
    private IntegrationIngestionService integrationIngestionService;

    @Autowired
    private IntegrationSourceRepository integrationSourceRepository;

    @Autowired
    private ExternalUserRepository externalUserRepository;

    @Autowired
    private ConversationRepository conversationRepository;

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
        conversationRepository.deleteAll();
        externalUserRepository.deleteAll();
        rawEventRepository.deleteAll();
        integrationSourceRepository.deleteAll();
    }

    @Test
    void storesRawAndNormalizedEntitiesOnFirstDelivery() {
        integrationIngestionService.ingest(validEvent());

        assertThat(integrationSourceRepository.findAll()).singleElement()
                .extracting(source -> source.getStatus())
                .isEqualTo(IntegrationStatus.ACTIVE);
        assertThat(externalUserRepository.findAll()).hasSize(1);
        assertThat(conversationRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(messageAttachmentRepository.findAll()).hasSize(1);
        assertThat(rawEventRepository.findAll()).singleElement()
                .extracting(rawEvent -> rawEvent.getProcessingStatus())
                .isEqualTo(ProcessingStatus.PERSISTED);
    }

    @Test
    void skipsDuplicateNormalizedWritesForSameEvent() {
        InboundIntegrationEvent event = validEvent();

        integrationIngestionService.ingest(event);
        integrationIngestionService.ingest(event);

        assertThat(messageRepository.findAll()).hasSize(1);
        assertThat(rawEventRepository.findAll()).hasSize(1);
        assertThat(messageAttachmentRepository.findAll()).hasSize(1);
    }

    @Test
    void keepsEntitiesSeparatedForDifferentSourcesOnSamePlatform() {
        integrationIngestionService.ingest(validEvent());
        integrationIngestionService.ingest(new InboundIntegrationEvent(
                Platform.VK,
                "message_new",
                "vk-2000000001-777-source-2",
                "{\"type\":\"message_new\"}",
                true,
                "84",
                "VK Demo 2",
                "{\"groupId\":84}",
                new InboundConversation("2000000001", ConversationType.GROUP, "Community chat", "{\"peerId\":2000000001}"),
                new InboundAuthor("123", "Ivan Ivanov", "ivan", "Ivan", "Ivanov", null, "https://vk.com/id123", false, "{\"city\":\"Moscow\"}"),
                new InboundMessage(
                        "777",
                        Instant.parse("2026-04-02T00:05:00Z"),
                        "Hello from VK source 2",
                        "hello from vk source 2",
                        MessageType.TEXT,
                        null,
                        null,
                        List.of()
                )
        ));

        assertThat(integrationSourceRepository.findAll()).hasSize(2);
        assertThat(externalUserRepository.findAll()).hasSize(2);
        assertThat(conversationRepository.findAll()).hasSize(2);
        assertThat(messageRepository.findAll()).hasSize(2);
    }

    @Test
    void marksRawEventAsFailedWhenNormalizationBreaks() {
        InboundIntegrationEvent invalid = new InboundIntegrationEvent(
                Platform.VK,
                "message_new",
                "vk-err-1",
                "{\"broken\":true}",
                true,
                "42",
                "Demo source",
                "{}",
                new InboundConversation("2000000001", ConversationType.GROUP, "Demo chat", "{}"),
                null,
                new InboundMessage(null, Instant.parse("2026-04-02T00:00:00Z"), null, null, MessageType.TEXT, null, null, List.of())
        );

        assertThatThrownBy(() -> integrationIngestionService.ingest(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalMessageId");

        assertThat(rawEventRepository.findByPlatformAndEventId(Platform.VK, "vk-err-1"))
                .isPresent()
                .get()
                .extracting(rawEvent -> rawEvent.getProcessingStatus())
                .isEqualTo(ProcessingStatus.FAILED);
    }

    private InboundIntegrationEvent validEvent() {
        return new InboundIntegrationEvent(
                Platform.VK,
                "message_new",
                "vk-2000000001-777",
                "{\"type\":\"message_new\"}",
                true,
                "42",
                "VK Demo",
                "{\"groupId\":42}",
                new InboundConversation("2000000001", ConversationType.GROUP, "Community chat", "{\"peerId\":2000000001}"),
                new InboundAuthor("123", "Ivan Ivanov", "ivan", "Ivan", "Ivanov", null, "https://vk.com/id123", false, "{\"city\":\"Moscow\"}"),
                new InboundMessage(
                        "777",
                        Instant.parse("2026-04-02T00:00:00Z"),
                        "Hello from VK",
                        "hello from vk",
                        MessageType.TEXT,
                        null,
                        null,
                        List.of(new InboundAttachment("photo", "photo-1", "https://example.com/1.jpg", "image/jpeg", "{}"))
                )
        );
    }
}
