package com.ca.centranalytics.integration.domain;

import com.ca.centranalytics.TestcontainersConfiguration;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import com.ca.centranalytics.integration.channel.vk.repository.VkCommentSnapshotRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkCrawlJobRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkUserCandidateRepository;
import com.ca.centranalytics.integration.channel.vk.repository.VkWallPostSnapshotRepository;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VkCrawlJobRepository vkCrawlJobRepository;

    @Autowired
    private VkGroupCandidateRepository vkGroupCandidateRepository;

    @Autowired
    private VkUserCandidateRepository vkUserCandidateRepository;

    @Autowired
    private VkWallPostSnapshotRepository vkWallPostSnapshotRepository;

    @Autowired
    private VkCommentSnapshotRepository vkCommentSnapshotRepository;

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
                .source(source)
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
        assertThat(messageRepository.findByConversationIdAndExternalMessageId(conversation.getId(), "777"))
                .isPresent()
                .get()
                .extracting(Message::getConversation)
                .extracting(Conversation::getTitle)
                .isEqualTo("Community chat");
    }

    @Test
    void persistsVkDiscoveryEntitiesAndRejectsDuplicates() {
        Instant now = Instant.parse("2026-04-06T00:00:00Z");

        jdbcTemplate.update("""
                        insert into vk_crawl_job (
                            job_type,
                            status,
                            request_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?)
                        """,
                "GROUP_SEARCH",
                "COMPLETED",
                "{\"region\":\"Primorsky Krai\"}",
                now,
                now);

        jdbcTemplate.update("""
                        insert into vk_group_candidate (
                            vk_group_id,
                            screen_name,
                            name,
                            region_match_source,
                            collection_method,
                            raw_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                1001L,
                "primorye_group",
                "Primorye Group",
                "STRUCTURED",
                "OFFICIAL_API",
                "{\"id\":1001,\"name\":\"Primorye Group\"}",
                now,
                now);

        jdbcTemplate.update("""
                        insert into vk_user_candidate (
                            vk_user_id,
                            display_name,
                            profile_url,
                            region_match_source,
                            collection_method,
                            raw_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                2002L,
                "Ivan Ivanov",
                "https://vk.com/id2002",
                "TEXT",
                "FALLBACK",
                "{\"id\":2002,\"first_name\":\"Ivan\",\"last_name\":\"Ivanov\"}",
                now,
                now);

        jdbcTemplate.update("""
                        insert into vk_wall_post_snapshot (
                            owner_id,
                            post_id,
                            author_vk_user_id,
                            text,
                            collection_method,
                            raw_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                -1001L,
                3003L,
                2002L,
                "Hello from Primorye",
                "OFFICIAL_API",
                "{\"owner_id\":-1001,\"id\":3003}",
                now,
                now);

        jdbcTemplate.update("""
                        insert into vk_comment_snapshot (
                            owner_id,
                            post_id,
                            comment_id,
                            author_vk_user_id,
                            text,
                            collection_method,
                            raw_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                -1001L,
                3003L,
                4004L,
                2002L,
                "Great post",
                "OFFICIAL_API",
                "{\"owner_id\":-1001,\"post_id\":3003,\"id\":4004}",
                now,
                now);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from vk_crawl_job where source_id is null",
                Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from vk_group_candidate where source_id is null",
                Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from vk_user_candidate where source_id is null",
                Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from vk_wall_post_snapshot where source_id is null",
                Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from vk_comment_snapshot where source_id is null",
                Long.class)).isEqualTo(1L);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        insert into vk_group_candidate (
                            vk_group_id,
                            screen_name,
                            name,
                            region_match_source,
                            collection_method,
                            raw_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                1001L,
                "primorye_group_dup",
                "Primorye Group Duplicate",
                "TEXT",
                "FALLBACK",
                "{\"id\":1001,\"name\":\"Primorye Group Duplicate\"}",
                now,
                now))
                .isInstanceOf(DuplicateKeyException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        insert into vk_user_candidate (
                            vk_user_id,
                            display_name,
                            profile_url,
                            region_match_source,
                            collection_method,
                            raw_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                2002L,
                "Ivan Ivanov Duplicate",
                "https://vk.com/id2002",
                "TEXT",
                "OFFICIAL_API",
                "{\"id\":2002,\"first_name\":\"Ivan\",\"last_name\":\"Ivanov\"}",
                now,
                now))
                .isInstanceOf(DuplicateKeyException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        insert into vk_wall_post_snapshot (
                            owner_id,
                            post_id,
                            author_vk_user_id,
                            text,
                            collection_method,
                            raw_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                -1001L,
                3003L,
                2002L,
                "Hello from Primorye duplicate",
                "FALLBACK",
                "{\"owner_id\":-1001,\"id\":3003}",
                now,
                now))
                .isInstanceOf(DuplicateKeyException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        insert into vk_comment_snapshot (
                            owner_id,
                            post_id,
                            comment_id,
                            author_vk_user_id,
                            text,
                            collection_method,
                            raw_json,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                -1001L,
                3003L,
                4004L,
                2002L,
                "Great post duplicate",
                "FALLBACK",
                "{\"owner_id\":-1001,\"post_id\":3003,\"id\":4004}",
                now,
                now))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void persistsVkDiscoveryEntitiesThroughRepositories() {
        VkCrawlJob crawlJob = vkCrawlJobRepository.saveAndFlush(VkCrawlJob.builder()
                .jobType(VkCrawlJobType.GROUP_SEARCH)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson("{\"region\":\"Primorsky Krai\"}")
                .build());

        VkGroupCandidate groupCandidate = vkGroupCandidateRepository.saveAndFlush(VkGroupCandidate.builder()
                .vkGroupId(1001L)
                .screenName("primorye_group")
                .name("Primorye Group")
                .regionMatchSource(VkMatchSource.STRUCTURED)
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"id\":1001}")
                .build());

        VkUserCandidate userCandidate = vkUserCandidateRepository.saveAndFlush(VkUserCandidate.builder()
                .vkUserId(2002L)
                .displayName("Ivan Ivanov")
                .firstName("Ivan")
                .lastName("Ivanov")
                .profileUrl("https://vk.com/id2002")
                .regionMatchSource(VkMatchSource.TEXT)
                .collectionMethod(VkCollectionMethod.FALLBACK)
                .rawJson("{\"id\":2002}")
                .build());

        VkWallPostSnapshot wallPostSnapshot = vkWallPostSnapshotRepository.saveAndFlush(VkWallPostSnapshot.builder()
                .ownerId(-1001L)
                .postId(3003L)
                .authorVkUserId(2002L)
                .text("Hello from Primorye")
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"owner_id\":-1001,\"id\":3003}")
                .build());

        VkCommentSnapshot commentSnapshot = vkCommentSnapshotRepository.saveAndFlush(VkCommentSnapshot.builder()
                .ownerId(-1001L)
                .postId(3003L)
                .commentId(4004L)
                .authorVkUserId(2002L)
                .text("Great post")
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"owner_id\":-1001,\"post_id\":3003,\"id\":4004}")
                .build());

        assertThat(crawlJob.getId()).isNotNull();
        assertThat(groupCandidate.getId()).isNotNull();
        assertThat(userCandidate.getId()).isNotNull();
        assertThat(wallPostSnapshot.getId()).isNotNull();
        assertThat(commentSnapshot.getId()).isNotNull();

        assertThat(vkCrawlJobRepository.findByStatus(VkCrawlJobStatus.CREATED)).hasSize(1);
        assertThat(vkGroupCandidateRepository.findByVkGroupId(1001L)).isPresent();
        assertThat(vkUserCandidateRepository.findByVkUserId(2002L)).isPresent();
        assertThat(vkWallPostSnapshotRepository.findByOwnerIdAndPostId(-1001L, 3003L)).isPresent();
        assertThat(vkCommentSnapshotRepository.findByOwnerIdAndPostIdAndCommentId(-1001L, 3003L, 4004L)).isPresent();
    }
}
