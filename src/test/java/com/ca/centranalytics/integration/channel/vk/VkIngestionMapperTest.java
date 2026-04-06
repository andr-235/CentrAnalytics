package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkCommentSnapshot;
import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.channel.vk.domain.VkWallPostSnapshot;
import com.ca.centranalytics.integration.channel.vk.service.VkAuthorNormalizationService;
import com.ca.centranalytics.integration.channel.vk.service.VkCommentIngestionMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkPostIngestionMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VkIngestionMapperTest {

    private final VkAuthorNormalizationService vkAuthorNormalizationService = new VkAuthorNormalizationService();
    private final VkPostIngestionMapper vkPostIngestionMapper = new VkPostIngestionMapper(vkAuthorNormalizationService);
    private final VkCommentIngestionMapper vkCommentIngestionMapper = new VkCommentIngestionMapper(vkAuthorNormalizationService);

    @Test
    void mapsWallPostSnapshotToInboundEvent() {
        VkGroupCandidate group = VkGroupCandidate.builder()
                .vkGroupId(1001L)
                .screenName("primorye_group")
                .name("Primorye Group")
                .regionMatchSource(VkMatchSource.STRUCTURED)
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"id\":1001}")
                .build();
        VkUserCandidate author = VkUserCandidate.builder()
                .vkUserId(2002L)
                .displayName("Ivan Ivanov")
                .firstName("Ivan")
                .lastName("Ivanov")
                .username("id2002")
                .profileUrl("https://vk.com/id2002")
                .mobilePhone("+79990000001")
                .regionMatchSource(VkMatchSource.TEXT)
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"id\":2002,\"counters\":{\"friends\":120}}")
                .build();
        VkWallPostSnapshot snapshot = VkWallPostSnapshot.builder()
                .ownerId(-1001L)
                .postId(3003L)
                .authorVkUserId(2002L)
                .text("Hello from Primorye")
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"owner_id\":-1001,\"id\":3003}")
                .build();

        var event = vkPostIngestionMapper.map(group, author, snapshot);

        assertThat(event.eventId()).isEqualTo("vk-wall--1001-3003");
        assertThat(event.sourceExternalId()).isEqualTo("1001");
        assertThat(event.conversation().externalConversationId()).isEqualTo("wall--1001");
        assertThat(event.message().externalMessageId()).isEqualTo("3003");
        assertThat(event.author().externalUserId()).isEqualTo("2002");
        assertThat(event.author().username()).isEqualTo("id2002");
        assertThat(event.author().phone()).isEqualTo("+79990000001");
        assertThat(event.author().metadataJson()).contains("\"friends\":120");
    }

    @Test
    void mapsCommentSnapshotToInboundEvent() {
        VkGroupCandidate group = VkGroupCandidate.builder()
                .vkGroupId(1001L)
                .screenName("primorye_group")
                .name("Primorye Group")
                .regionMatchSource(VkMatchSource.STRUCTURED)
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"id\":1001}")
                .build();
        VkUserCandidate author = VkUserCandidate.builder()
                .vkUserId(2002L)
                .displayName("Ivan Ivanov")
                .firstName("Ivan")
                .lastName("Ivanov")
                .username("id2002")
                .profileUrl("https://vk.com/id2002")
                .mobilePhone("+79990000001")
                .regionMatchSource(VkMatchSource.TEXT)
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"id\":2002}")
                .build();
        VkCommentSnapshot snapshot = VkCommentSnapshot.builder()
                .ownerId(-1001L)
                .postId(3003L)
                .commentId(4004L)
                .authorVkUserId(2002L)
                .text("Great post")
                .collectionMethod(VkCollectionMethod.OFFICIAL_API)
                .rawJson("{\"owner_id\":-1001,\"post_id\":3003,\"id\":4004}")
                .build();

        var event = vkCommentIngestionMapper.map(group, author, snapshot);

        assertThat(event.eventId()).isEqualTo("vk-comment--1001-3003-4004");
        assertThat(event.conversation().externalConversationId()).isEqualTo("wall--1001-3003");
        assertThat(event.message().externalMessageId()).isEqualTo("4004");
        assertThat(event.message().replyToExternalMessageId()).isEqualTo("3003");
    }
}
