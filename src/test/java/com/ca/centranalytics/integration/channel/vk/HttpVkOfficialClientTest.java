package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.client.HttpVkOfficialClient;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpVkOfficialClientTest {

    private HttpServer server;
    private String baseUrl;
    private final Map<String, String> lastQueryByMethod = new HashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/method/groups.search", exchange -> respond(exchange, "groups.search", """
                {
                  "response": {
                    "count": 1,
                    "items": [
                      {
                        "id": 1001,
                        "name": "Primorye Group",
                        "screen_name": "primorye_group",
                        "description": "News from Primorsky Krai",
                        "city": { "title": "Vladivostok" }
                      }
                    ]
                  }
                }
                """));
        server.createContext("/method/users.search", exchange -> respond(exchange, "users.search", """
                {
                  "response": {
                    "count": 1,
                    "items": [
                      {
                        "id": 2002,
                        "first_name": "Ivan",
                        "last_name": "Ivanov",
                        "screen_name": "id2002",
                        "city": { "title": "Vladivostok" }
                      }
                    ]
                  }
                }
                """));
        server.createContext("/method/wall.get", exchange -> respond(exchange, "wall.get", """
                {
                  "response": {
                    "count": 1,
                    "items": [
                      {
                        "id": 3003,
                        "owner_id": -1001,
                        "from_id": 2002,
                        "text": "Hello from Primorye",
                        "date": 1712361600
                      }
                    ]
                  }
                }
                """));
        server.createContext("/method/wall.getComments", exchange -> respond(exchange, "wall.getComments", """
                {
                  "response": {
                    "count": 1,
                    "items": [
                      {
                        "id": 4004,
                        "from_id": 3003,
                        "text": "Great post",
                        "date": 1712365200
                      }
                    ]
                  }
                }
                """));
        server.createContext("/method/users.get", exchange -> respond(exchange, "users.get", """
                {
                  "response": [
                    {
                      "id": 2002,
                      "first_name": "Ivan",
                      "last_name": "Ivanov",
                      "domain": "id2002",
                      "home_town": "Vladivostok"
                    },
                    {
                      "id": 3003,
                      "first_name": "Petr",
                      "last_name": "Petrov",
                      "screen_name": "id3003",
                      "city": { "title": "Artem" }
                    }
                  ]
                }
                """));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/method";
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void searchesGroupsThroughOfficialApi() {
        HttpVkOfficialClient client = new HttpVkOfficialClient(
                RestClient.builder(),
                properties("vk-token", "vk-user-token"),
                new ObjectMapper()
        );

        var results = client.searchGroups("Primorsky Krai", 25);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.id()).isEqualTo(1001L);
            assertThat(result.name()).isEqualTo("Primorye Group");
            assertThat(result.screenName()).isEqualTo("primorye_group");
            assertThat(result.city()).isEqualTo("Vladivostok");
        });
        assertThat(lastQueryByMethod.get("groups.search")).contains("access_token=vk-token");
        assertThat(lastQueryByMethod.get("groups.search")).contains("q=Primorsky");
    }

    @Test
    void searchesUsersWithUserTokenAndBuildsProfileUrl() {
        HttpVkOfficialClient client = new HttpVkOfficialClient(
                RestClient.builder(),
                properties("vk-token", "vk-user-token"),
                new ObjectMapper()
        );

        var results = client.searchUsers("Primorsky Krai", 10);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.displayName()).isEqualTo("Ivan Ivanov");
            assertThat(result.profileUrl()).isEqualTo("https://vk.com/id2002");
            assertThat(result.city()).isEqualTo("Vladivostok");
        });
        assertThat(lastQueryByMethod.get("users.search")).contains("access_token=vk-user-token");
    }

    @Test
    void loadsPostsCommentsAndUsersByIds() {
        HttpVkOfficialClient client = new HttpVkOfficialClient(
                RestClient.builder(),
                properties("vk-token", ""),
                new ObjectMapper()
        );

        var posts = client.getGroupPosts(1001L, 5);
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
        assertThat(lastQueryByMethod.get("wall.get")).contains("owner_id=-1001");
        assertThat(lastQueryByMethod.get("users.get")).contains("user_ids=2002,3003");
        assertThat(lastQueryByMethod.get("users.get")).contains("access_token=vk-token");
    }

    private VkProperties properties(String accessToken, String userAccessToken) {
        return new VkProperties(
                42L,
                "vk-secret",
                "vk-confirm",
                accessToken,
                userAccessToken,
                "/api/integrations/webhooks/vk",
                "5.199",
                baseUrl,
                Duration.ofSeconds(5)
        );
    }

    private void respond(HttpExchange exchange, String methodName, String payload) throws IOException {
        lastQueryByMethod.put(methodName, exchange.getRequestURI().getRawQuery());
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
