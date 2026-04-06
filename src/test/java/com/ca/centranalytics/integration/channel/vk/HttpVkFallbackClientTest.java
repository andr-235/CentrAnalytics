package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.client.HttpVkFallbackClient;
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
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpVkFallbackClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/search", this::respondSearch);
        server.createContext("/public1001", exchange -> respond(exchange, """
                <html>
                  <body>
                    <div class="wall_post" data-post-id="-1001_3003" data-from-id="2002">
                      <div class="wall_post_text">Hello from Primorye</div>
                      <time datetime="2026-04-06T00:00:00Z"></time>
                    </div>
                    <div class="reply" id="reply4004" data-post-id="3003">
                      <a class="reply_author" href="/id3003">Petr Petrov</a>
                      <div class="reply_text">Great post</div>
                      <time datetime="2026-04-06T01:00:00Z"></time>
                    </div>
                  </body>
                </html>
                """));
        server.createContext("/public1002", exchange -> respond(exchange, """
                <html>
                  <body>
                    <div class="post" data-post-id="-1002_5005">
                      <a class="post_author" href="/id2004">Anna Smirnova</a>
                      <div class="post_text">Alt layout post text</div>
                      <span data-time="1770000000"></span>
                    </div>
                  </body>
                </html>
                """));
        server.createContext("/wall-1001_3003", exchange -> respond(exchange, """
                <html>
                  <body>
                    <div class="reply" id="reply4004" data-post-id="3003">
                      <a class="reply_author" href="/id3003">Petr Petrov</a>
                      <div class="reply_text">Great post</div>
                      <time datetime="2026-04-06T01:00:00Z"></time>
                    </div>
                  </body>
                </html>
                """));
        server.createContext("/wall-1002_5005", exchange -> respond(exchange, """
                <html>
                  <body>
                    <div class="wall_reply" data-reply-id="7007">
                      <a class="wall_reply_author" href="/id2004">Anna Smirnova</a>
                      <div class="wall_reply_text">Alt reply text</div>
                      <span data-time="1770003600"></span>
                    </div>
                  </body>
                </html>
                """));
        server.createContext("/id2002", exchange -> respond(exchange, """
                <html>
                  <body>
                    <h1 class="page_name">Ivan Ivanov</h1>
                    <div class="profile_status">online</div>
                    <img class="page_avatar_img" src="https://vk.com/images/2002.jpg" />
                    <div class="profile_info_row">
                      <div class="label">Город:</div>
                      <div class="value">Vladivostok</div>
                    </div>
                    <div class="profile_info_row">
                      <div class="label">Родной город:</div>
                      <div class="value">Arsenyev</div>
                    </div>
                    <div class="profile_info_row">
                      <div class="label">Дата рождения:</div>
                      <div class="value">10.10.1990</div>
                    </div>
                    <script type="application/ld+json">
                      {"name":"Ivan Ivanov","alternateName":"id2002","image":"https://vk.com/images/2002.jpg"}
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/id3003", exchange -> respond(exchange, """
                <html>
                  <body>
                    <h1 class="page_name">Petr Petrov</h1>
                    <div class="profile_info_row">
                      <div class="label">Город:</div>
                      <div class="value">Artem</div>
                    </div>
                  </body>
                </html>
                """));
        server.createContext("/id2004", exchange -> respond(exchange, """
                <html>
                  <head>
                    <meta property="og:image" content="https://vk.com/images/2004.jpg" />
                  </head>
                  <body>
                    <h1>Anna Smirnova</h1>
                    <div class="profile_status">active</div>
                    <div class="profile_info_row">
                      <div class="label">Город:</div>
                      <div class="value">Ussuriysk</div>
                    </div>
                    <div class="profile_info_row">
                      <div class="label">Дата рождения:</div>
                      <div class="value">01.01.1995</div>
                    </div>
                    <div class="profile_info_row">
                      <div class="label">Пол:</div>
                      <div class="value">женский</div>
                    </div>
                    <div class="profile_info_row">
                      <div class="label">Сайт:</div>
                      <div class="value">https://anna.example.com</div>
                    </div>
                    <div class="profile_info_row">
                      <div class="label">Моб. телефон:</div>
                      <div class="value">+79990000004</div>
                    </div>
                    <div class="profile_info_row">
                      <div class="label">Дом. телефон:</div>
                      <div class="value">84232000004</div>
                    </div>
                    <div class="profile_info_row">
                      <div class="label">Образование:</div>
                      <div class="value">VGUES</div>
                    </div>
                  </body>
                </html>
                """));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void searchesGroupsAndUsersViaFallbackPages() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), properties());

        var groups = client.searchGroups("Primorsky Krai", 10);
        var users = client.searchUsers("Primorsky Krai", 10);

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1001L);
            assertThat(group.name()).isEqualTo("Primorye Group");
            assertThat(group.screenName()).isEqualTo("public1001");
            assertThat(group.description()).contains("Primorsky Krai");
            assertThat(group.city()).isEqualTo("Vladivostok");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2002L);
            assertThat(user.displayName()).isEqualTo("Ivan Ivanov");
            assertThat(user.firstName()).isEqualTo("Ivan");
            assertThat(user.lastName()).isEqualTo("Ivanov");
            assertThat(user.username()).isEqualTo("id2002");
            assertThat(user.profileUrl()).isEqualTo("https://vk.com/id2002");
            assertThat(user.city()).isEqualTo("Vladivostok");
            assertThat(user.homeTown()).isEqualTo("Arsenyev");
        });
    }

    @Test
    void collectsPostsCommentsAndProfilesViaFallbackPages() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), properties());

        var posts = client.getGroupPosts(1001L, 10);
        var comments = client.getPostComments(-1001L, 3003L, 10);
        var users = client.getUsersByIds(List.of(2002L, 3003L));

        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.ownerId()).isEqualTo(-1001L);
            assertThat(post.postId()).isEqualTo(3003L);
            assertThat(post.authorVkUserId()).isEqualTo(2002L);
            assertThat(post.text()).isEqualTo("Hello from Primorye");
            assertThat(post.createdAt()).isEqualTo(Instant.parse("2026-04-06T00:00:00Z"));
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.ownerId()).isEqualTo(-1001L);
            assertThat(comment.postId()).isEqualTo(3003L);
            assertThat(comment.commentId()).isEqualTo(4004L);
            assertThat(comment.authorVkUserId()).isEqualTo(3003L);
            assertThat(comment.text()).isEqualTo("Great post");
            assertThat(comment.createdAt()).isEqualTo(Instant.parse("2026-04-06T01:00:00Z"));
        });
        assertThat(users).extracting(user -> user.id()).containsExactly(2002L, 3003L);
        assertThat(users).extracting(user -> user.city()).containsExactly("Vladivostok", "Artem");
        assertThat(users).extracting(user -> user.birthDate()).containsExactly("10.10.1990", null);
        assertThat(users).extracting(user -> user.status()).containsExactly("online", null);
        assertThat(users).extracting(user -> user.avatarUrl()).containsExactly("https://vk.com/images/2002.jpg", null);
    }

    @Test
    void parsesAlternativeMarkupAndRicherProfileFields() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), properties());

        var posts = client.getGroupPosts(1002L, 10);
        var comments = client.getPostComments(-1002L, 5005L, 10);
        var users = client.getUsersByIds(List.of(2004L));

        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(5005L);
            assertThat(post.authorVkUserId()).isEqualTo(2004L);
            assertThat(post.text()).isEqualTo("Alt layout post text");
            assertThat(post.createdAt()).isEqualTo(Instant.ofEpochSecond(1770000000));
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(7007L);
            assertThat(comment.authorVkUserId()).isEqualTo(2004L);
            assertThat(comment.text()).isEqualTo("Alt reply text");
            assertThat(comment.createdAt()).isEqualTo(Instant.ofEpochSecond(1770003600));
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Anna Smirnova");
            assertThat(user.city()).isEqualTo("Ussuriysk");
            assertThat(user.birthDate()).isEqualTo("01.01.1995");
            assertThat(user.sex()).isEqualTo(1);
            assertThat(user.site()).isEqualTo("https://anna.example.com");
            assertThat(user.mobilePhone()).isEqualTo("+79990000004");
            assertThat(user.homePhone()).isEqualTo("84232000004");
            assertThat(user.education()).isEqualTo("VGUES");
            assertThat(user.avatarUrl()).isEqualTo("https://vk.com/images/2004.jpg");
        });
    }

    private VkProperties properties() {
        return new VkProperties(
                42L,
                "vk-secret",
                "vk-confirm",
                "vk-token",
                "vk-user-token",
                "/api/integrations/webhooks/vk",
                "5.199",
                "https://api.vk.com/method",
                baseUrl,
                Duration.ofSeconds(5)
        );
    }

    private void respondSearch(HttpExchange exchange) throws IOException {
        String query = firstNonBlank(exchange.getRequestURI().getQuery(), exchange.getRequestURI().getRawQuery());
        if (query != null && query.contains("communities")) {
            respond(exchange, """
                    <html>
                      <body>
                        <div class="groups_row">
                          <a class="groups_row_title" href="/public1001">Primorye Group</a>
                          <div class="groups_row_description">News from Primorsky Krai</div>
                          <div class="groups_row_city">Vladivostok</div>
                        </div>
                      </body>
                    </html>
                    """);
            return;
        }
        respond(exchange, """
                <html>
                  <body>
                    <div class="people_row">
                      <a class="people_row_photo" href="/id2002"><img src="https://vk.com/images/2002.jpg" /></a>
                      <a class="people_row_name" href="/id2002">Ivan Ivanov</a>
                      <div class="people_row_city">Vladivostok</div>
                      <div class="people_row_details">Arsenyev</div>
                    </div>
                  </body>
                </html>
                """);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void respond(HttpExchange exchange, String payload) throws IOException {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
