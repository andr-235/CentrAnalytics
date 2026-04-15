package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.domain.VkCollectionMethod;
import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.service.VkCommentSnapshotMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkOfficialGroupCandidateMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkOfficialUserCandidateMapper;
import com.ca.centranalytics.integration.channel.vk.service.VkRegionMatcher;
import com.ca.centranalytics.integration.channel.vk.service.VkWallSnapshotMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VkOfficialMapperTest {

    private final VkRegionMatcher regionMatcher = new VkRegionMatcher();
    private final VkOfficialGroupCandidateMapper groupMapper = new VkOfficialGroupCandidateMapper(regionMatcher);
    private final VkOfficialUserCandidateMapper userMapper = new VkOfficialUserCandidateMapper(regionMatcher);
    private final VkWallSnapshotMapper wallSnapshotMapper = new VkWallSnapshotMapper();
    private final VkCommentSnapshotMapper commentSnapshotMapper = new VkCommentSnapshotMapper();

    @Test
    void mapsOfficialGroupSearchResult() {
        VkGroupSearchResult result = new VkGroupSearchResult(
                1001L,
                "Primorye Group",
                "primorye_group",
                "News from Primorsky Krai",
                "Vladivostok",
                "{\"id\":1001}"
        );

        var candidate = groupMapper.map("Primorsky Krai", result, VkCollectionMethod.OFFICIAL_API);

        assertThat(candidate.getVkGroupId()).isEqualTo(1001L);
        assertThat(candidate.getCollectionMethod()).isEqualTo(VkCollectionMethod.OFFICIAL_API);
        assertThat(candidate.getRegionMatchSource()).isEqualTo(VkMatchSource.TEXT);
        assertThat(candidate.getRawJson()).isEqualTo("{\"id\":1001}");
    }

    @Test
    void mapsOfficialUserSearchResult() {
        VkUserSearchResult result = new VkUserSearchResult(
                2002L,
                "Ivan Ivanov",
                "Ivan",
                "Ivanov",
                "id2002",
                "https://vk.com/id2002",
                "Primorsky Krai",
                "Vladivostok",
                "10.10.1990",
                2,
                "online",
                Instant.parse("2026-04-06T00:00:00Z"),
                "https://vk.com/images/2002.jpg",
                "+79990000001",
                "84232000000",
                "https://example.com",
                1,
                "FEFU",
                "{\"company\":\"CA\"}",
                "{\"friends\":120}",
                "{\"id\":2002}"
        );

        var candidate = userMapper.map("Primorsky Krai", result, VkCollectionMethod.OFFICIAL_API);

        assertThat(candidate.getVkUserId()).isEqualTo(2002L);
        assertThat(candidate.getCollectionMethod()).isEqualTo(VkCollectionMethod.OFFICIAL_API);
        assertThat(candidate.getRegionMatchSource()).isEqualTo(VkMatchSource.STRUCTURED);
        assertThat(candidate.getUsername()).isEqualTo("id2002");
        assertThat(candidate.getProfileUrl()).isEqualTo("https://vk.com/id2002");
        assertThat(candidate.getHomeTown()).isEqualTo("Vladivostok");
        assertThat(candidate.getBirthDate()).isEqualTo("10.10.1990");
        assertThat(candidate.getSex()).isEqualTo(2);
        assertThat(candidate.getLastSeenAt()).isEqualTo(Instant.parse("2026-04-06T00:00:00Z"));
        assertThat(candidate.getAvatarUrl()).isEqualTo("https://vk.com/images/2002.jpg");
        assertThat(candidate.getMobilePhone()).isEqualTo("+79990000001");
        assertThat(candidate.getHomePhone()).isEqualTo("84232000000");
        assertThat(candidate.getSite()).isEqualTo("https://example.com");
        assertThat(candidate.getRelation()).isEqualTo(1);
        assertThat(candidate.getEducation()).isEqualTo("FEFU");
        assertThat(candidate.getCareerJson()).isEqualTo("{\"company\":\"CA\"}");
        assertThat(candidate.getCountersJson()).isEqualTo("{\"friends\":120}");
    }

    @Test
    void mapsWallPostSnapshot() {
        VkWallPostResult result = new VkWallPostResult(
                -1001L,
                3003L,
                2002L,
                "Hello from Primorye",
                Instant.parse("2026-04-06T00:00:00Z"),
                "{\"owner_id\":-1001,\"id\":3003}"
        );

        var snapshot = wallSnapshotMapper.map(result, VkCollectionMethod.OFFICIAL_API);

        assertThat(snapshot.getOwnerId()).isEqualTo(-1001L);
        assertThat(snapshot.getPostId()).isEqualTo(3003L);
        assertThat(snapshot.getCollectionMethod()).isEqualTo(VkCollectionMethod.OFFICIAL_API);
        assertThat(snapshot.getRawJson()).isEqualTo("{\"owner_id\":-1001,\"id\":3003}");
    }

    @Test
    void mapsCommentSnapshot() {
        VkCommentResult result = new VkCommentResult(
                -1001L,
                3003L,
                4004L,
                2002L,
                "Great post",
                Instant.parse("2026-04-06T00:00:00Z"),
                "{\"owner_id\":-1001,\"post_id\":3003,\"id\":4004}"
        );

        var snapshot = commentSnapshotMapper.map(result, VkCollectionMethod.OFFICIAL_API);

        assertThat(snapshot.getCommentId()).isEqualTo(4004L);
        assertThat(snapshot.getCollectionMethod()).isEqualTo(VkCollectionMethod.OFFICIAL_API);
        assertThat(snapshot.getAuthorVkUserId()).isEqualTo(2002L);
    }
}
