package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.client.HttpVkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpVkOfficialClientTest {

    @Test
    void searchesGroupsThroughOfficialApi() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        HttpVkOfficialClient client = HttpVkOfficialClient.withVkApiClient(
                properties("vk-token", "vk-user-token", null),
                new VkApiClient(transportClient)
        );

        var results = client.searchGroups("Primorsky Krai", 25);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.id()).isEqualTo(1001L);
            assertThat(result.name()).isEqualTo("Primorye Group");
            assertThat(result.screenName()).isEqualTo("primorye_group");
            assertThat(result.city()).isEqualTo("Vladivostok");
        });
        assertThat(transportClient.lastRequestByMethod.get("groups.search")).contains("access_token=vk-token");
        assertThat(transportClient.lastRequestByMethod.get("groups.search")).contains("q=Primorsky");
    }

    @Test
    void resolvesRegionalSearchTermsForEaoFromVkDatabase() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        HttpVkOfficialClient client = HttpVkOfficialClient.withVkApiClient(
                properties("vk-token", "vk-user-token", null),
                new VkApiClient(transportClient)
        );

        var terms = client.resolveRegionalSearchTerms("Еврейская автономная область");

        assertThat(terms).containsExactly("Биробиджан", "Облучье");
        assertThat(transportClient.lastRequestByMethod.get("database.getRegions")).contains("q=%D0%95%D0%B2%D1%80%D0%B5%D0%B9%D1%81%D0%BA%D0%B0%D1%8F");
        assertThat(transportClient.lastRequestByMethod.get("database.getCities")).contains("region_id=77");
        assertThat(transportClient.lastRequestByMethod.get("database.getCities")).contains("need_all=1");
    }

    @Test
    void searchesUsersWithUserTokenAndBuildsProfileUrl() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        HttpVkOfficialClient client = HttpVkOfficialClient.withVkApiClient(
                properties("vk-token", "vk-user-token", null),
                new VkApiClient(transportClient)
        );

        var results = client.searchUsers("Primorsky Krai", 10);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.displayName()).isEqualTo("Ivan Ivanov");
            assertThat(result.username()).isEqualTo("id2002");
            assertThat(result.profileUrl()).isEqualTo("https://vk.com/id2002");
            assertThat(result.city()).isEqualTo("Vladivostok");
            assertThat(result.homeTown()).isEqualTo("Arsenyev");
            assertThat(result.birthDate()).isEqualTo("10.10.1990");
            assertThat(result.sex()).isEqualTo(2);
            assertThat(result.status()).isEqualTo("online");
            assertThat(result.lastSeenAt()).isEqualTo(java.time.Instant.ofEpochSecond(1712400000));
            assertThat(result.avatarUrl()).isEqualTo("https://vk.com/images/2002.jpg");
            assertThat(result.mobilePhone()).isEqualTo("+79990000001");
            assertThat(result.homePhone()).isEqualTo("84232000000");
            assertThat(result.site()).isEqualTo("https://example.com");
            assertThat(result.relation()).isEqualTo(1);
            assertThat(result.education()).isEqualTo("FEFU");
            assertThat(result.careerJson()).contains("\"company\":\"CA\"");
            assertThat(result.countersJson()).contains("\"friends\":120");
        });
        assertThat(transportClient.lastRequestByMethod.get("users.search")).contains("access_token=vk-user-token");
    }

    @Test
    void loadsPostsCommentsAndUsersByIds() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        HttpVkOfficialClient client = HttpVkOfficialClient.withVkApiClient(
                properties("vk-token", "", null),
                new VkApiClient(transportClient)
        );

        var posts = client.getGroupPosts("club1001", 5);
        var comments = client.getPostComments(-1001L, 3003L, 5);
        var users = client.getUsersByIds(List.of(2002L, 3003L));

        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.ownerId()).isEqualTo(-1001L);
            assertThat(post.authorVkUserId()).isEqualTo(2002L);
            assertThat(post.createdAt()).isNotNull();
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(4004L);
            assertThat(comment.authorVkUserId()).isEqualTo(3003L);
            assertThat(comment.createdAt()).isNotNull();
        });
        assertThat(users).extracting(user -> user.profileUrl())
                .containsExactly("https://vk.com/id2002", "https://vk.com/id3003");
        assertThat(users).extracting(user -> user.username())
                .containsExactly("id2002", "id3003");
        assertThat(users).extracting(user -> user.education())
                .containsExactly("FEFU", null);
        assertThat(transportClient.lastRequestByMethod.get("wall.get")).contains("domain=club1001");
        assertThat(transportClient.lastRequestByMethod.get("users.get")).contains("user_ids=2002%2C3003");
        assertThat(transportClient.lastRequestByMethod.get("users.get")).contains("access_token=vk-token");
    }

    private VkProperties properties(String accessToken, String userAccessToken, String apiBaseUrl) {
        return new VkProperties(
                42L,
                accessToken,
                userAccessToken,
                "5.199",
                apiBaseUrl,
                "https://vk.com",
                Duration.ofSeconds(5)
        );
    }

    static final class RecordingTransportClient implements TransportClient {

        private final Map<String, String> lastUrlByMethod = new HashMap<>();
        private final Map<String, String> lastRequestByMethod = new HashMap<>();

        @Override
        public ClientResponse get(String url) throws IOException {
            return record(url, null);
        }

        @Override
        public ClientResponse get(String url, String body) throws IOException {
            return get(url);
        }

        @Override
        public ClientResponse get(String url, org.apache.http.Header[] headers) throws IOException {
            return get(url);
        }

        @Override
        public ClientResponse post(String url, String body) throws IOException {
            return record(url, body);
        }

        @Override
        public ClientResponse post(String url, Map<String, java.io.File> files) throws IOException {
            return record(url, null);
        }

        @Override
        public ClientResponse post(String url, String body, String contentType) throws IOException {
            return record(url, body);
        }

        @Override
        public ClientResponse post(String url) throws IOException {
            return record(url, null);
        }

        @Override
        public ClientResponse post(String url, String body, org.apache.http.Header[] headers) throws IOException {
            return record(url, body);
        }

        @Override
        public ClientResponse delete(String url) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientResponse delete(String url, String body) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientResponse delete(String url, String body, String contentType) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientResponse delete(String url, String body, org.apache.http.Header[] headers) throws IOException {
            throw new UnsupportedOperationException();
        }

        private String payload(String methodName) {
            return switch (methodName) {
                case "groups.search" -> """
                        {"response":{"count":1,"items":[{"id":1001,"name":"Primorye Group","screen_name":"primorye_group","description":"News from Primorsky Krai","city":{"title":"Vladivostok"}}]}}
                        """;
                case "users.search" -> """
                        {"response":{"count":1,"items":[{"id":2002,"first_name":"Ivan","last_name":"Ivanov","screen_name":"id2002","city":{"title":"Vladivostok"},"home_town":"Arsenyev","bdate":"10.10.1990","sex":2,"status":"online","last_seen":{"time":1712400000},"photo_200":"https://vk.com/images/2002.jpg","mobile_phone":"+79990000001","home_phone":"84232000000","site":"https://example.com","relation":1,"university_name":"FEFU","career":[{"company":"CA"}],"counters":{"friends":120}}]}}
                        """;
                case "wall.get" -> """
                        {"response":{"count":1,"items":[{"id":3003,"owner_id":-1001,"from_id":2002,"text":"Hello from Primorye","date":1712361600}]}}
                        """;
                case "wall.getComments" -> """
                        {"response":{"count":1,"items":[{"id":4004,"owner_id":-1001,"post_id":3003,"from_id":3003,"text":"Great post","date":1712365200}]}}
                        """;
                case "users.get" -> """
                        {"response":[{"id":2002,"first_name":"Ivan","last_name":"Ivanov","domain":"id2002","home_town":"Vladivostok","bdate":"10.10.1990","sex":2,"status":"online","last_seen":{"time":1712400000},"photo_200":"https://vk.com/images/2002.jpg","mobile_phone":"+79990000001","home_phone":"84232000000","site":"https://example.com","relation":1,"university_name":"FEFU","career":[{"company":"CA"}],"counters":{"friends":120}},{"id":3003,"first_name":"Petr","last_name":"Petrov","screen_name":"id3003","city":{"title":"Artem"},"career":[],"counters":{"followers":2}}]}
                        """;
                case "database.getRegions" -> """
                        {"response":{"count":1,"items":[{"id":77,"title":"Еврейская автономная область"}]}}
                        """;
                case "database.getCities" -> """
                        {"response":{"count":2,"items":[{"id":1,"title":"Биробиджан","region":"Еврейская автономная область"},{"id":2,"title":"Облучье","region":"Еврейская автономная область"}]}}
                        """;
                default -> throw new IllegalStateException("Unexpected method: " + methodName);
            };
        }

        private ClientResponse record(String url, String body) {
            String methodName = methodName(url);
            lastUrlByMethod.put(methodName, url);
            lastRequestByMethod.put(methodName, body == null ? url : url + "?" + body);
            return new ClientResponse(200, payload(methodName), Map.of("Content-Type", "application/json"));
        }

        private String methodName(String url) {
            int methodIndex = url.indexOf("/method/");
            String tail = methodIndex >= 0 ? url.substring(methodIndex + "/method/".length()) : url;
            int queryIndex = tail.indexOf('?');
            return queryIndex >= 0 ? tail.substring(0, queryIndex) : tail;
        }
    }
}
