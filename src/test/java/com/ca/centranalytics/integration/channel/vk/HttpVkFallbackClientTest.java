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
        server.createContext("/script/search", this::respondSearch);
        server.createContext("/inline/search", this::respondSearch);
        server.createContext("/nested/search", this::respondSearch);
        server.createContext("/mapped/search", this::respondSearch);
        server.createContext("/lenient/search", this::respondSearch);
        server.createContext("/commented/search", this::respondSearch);
        server.createContext("/jsonparse/search", this::respondSearch);
        server.createContext("/datablob/search", this::respondSearch);
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
        server.createContext("/public1003", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script id="vk_payload" type="application/json">
                      {
                        "posts": [
                          {
                            "post_id": 9009,
                            "owner_id": -1003,
                            "from_id": 2005,
                            "text": "Post from script payload",
                            "created_at": "2026-04-07T00:00:00Z"
                          }
                        ]
                      }
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/script/public1003", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script id="vk_payload" type="application/json">
                      {
                        "posts": [
                          {
                            "post_id": 9009,
                            "owner_id": -1003,
                            "from_id": 2005,
                            "text": "Post from script payload",
                            "created_at": "2026-04-07T00:00:00Z"
                          }
                        ]
                      }
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/inline/public1004", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "posts": [
                          {
                            "post_id": 9011,
                            "owner_id": -1004,
                            "from_id": 2006,
                            "text": "Post from inline script payload",
                            "created_at": "2026-04-07T02:00:00Z"
                          }
                        ]
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/nested/public1005", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "payload": {
                          "feed": {
                            "items": {
                              "posts": [
                                {
                                  "post_id": 9013,
                                  "owner_id": -1005,
                                  "from_id": 2007,
                                  "text": "Post from nested payload",
                                  "created_at": "2026-04-07T04:00:00Z"
                                }
                              ]
                            }
                          }
                        }
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/mapped/public1006", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "feed": {
                          "events": [
                            {"type": "meta", "value": "ignored"},
                            {
                              "type": "posts",
                              "items": {
                                "501": {
                                  "post_id": 9015,
                                  "owner_id": -1006,
                                  "from_id": 2008,
                                  "text": "Post from mapped payload",
                                  "created_at": "2026-04-07T06:00:00Z"
                                }
                              }
                            }
                          ]
                        }
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/lenient/public1007", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        'posts': [
                          {
                            'post_id': 9017,
                            'owner_id': -1007,
                            'from_id': 2009,
                            'text': 'Post from lenient payload',
                            'created_at': '2026-04-07T08:00:00Z',
                            'extra': undefined,
                          },
                        ],
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/commented/public1010", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        posts: [
                          {
                            post_id: 9020,
                            owner_id: -1010,
                            from_id: 2010,
                            text: 'Post from commented payload', // post text
                            created_at: '2026-04-07T10:00:00Z',
                          },
                        ],
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/jsonparse/public1011", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = JSON.parse("{\"posts\":[{\"post_id\":9022,\"owner_id\":-1011,\"from_id\":2011,\"text\":\"Post from json parse payload\",\"created_at\":\"2026-04-07T12:00:00Z\"}]}");
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/datablob/public1012", exchange -> respond(exchange, """
                <html>
                  <body>
                    <div id="page_wall_posts" data-state="{&quot;posts&quot;:[{&quot;post_id&quot;:9024,&quot;owner_id&quot;:-1012,&quot;from_id&quot;:2012,&quot;text&quot;:&quot;Post from data blob payload&quot;,&quot;created_at&quot;:&quot;2026-04-07T14:00:00Z&quot;}]}"></div>
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
        server.createContext("/wall-1003_9009", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script id="vk_payload" type="application/json">
                      {
                        "comments": [
                          {
                            "comment_id": 9010,
                            "post_id": 9009,
                            "owner_id": -1003,
                            "from_id": 2005,
                            "text": "Comment from script payload",
                            "created_at": "2026-04-07T01:00:00Z"
                          }
                        ]
                      }
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/script/wall-1003_9009", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script id="vk_payload" type="application/json">
                      {
                        "comments": [
                          {
                            "comment_id": 9010,
                            "post_id": 9009,
                            "owner_id": -1003,
                            "from_id": 2005,
                            "text": "Comment from script payload",
                            "created_at": "2026-04-07T01:00:00Z"
                          }
                        ]
                      }
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/inline/wall-1004_9011", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "comments": [
                          {
                            "comment_id": 9012,
                            "post_id": 9011,
                            "owner_id": -1004,
                            "from_id": 2006,
                            "text": "Comment from inline script payload",
                            "created_at": "2026-04-07T03:00:00Z"
                          }
                        ]
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/nested/wall-1005_9013", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "payload": {
                          "commentsBlock": {
                            "items": {
                              "comments": [
                                {
                                  "comment_id": 9014,
                                  "post_id": 9013,
                                  "owner_id": -1005,
                                  "from_id": 2007,
                                  "text": "Comment from nested payload",
                                  "created_at": "2026-04-07T05:00:00Z"
                                }
                              ]
                            }
                          }
                        }
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/mapped/wall-1006_9015", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "replies": {
                          "events": [
                            {"kind": "summary"},
                            {
                              "kind": "comments",
                              "items": {
                                "601": {
                                  "comment_id": 9016,
                                  "post_id": 9015,
                                  "owner_id": -1006,
                                  "from_id": 2008,
                                  "text": "Comment from mapped payload",
                                  "created_at": "2026-04-07T07:00:00Z"
                                }
                              }
                            }
                          ]
                        }
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/lenient/wall-1007_9017", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        'comments': [
                          {
                            'comment_id': 9018,
                            'post_id': 9017,
                            'owner_id': -1007,
                            'from_id': 2009,
                            'text': 'Comment from lenient payload',
                            'created_at': '2026-04-07T09:00:00Z',
                            'unused': undefined,
                          },
                        ],
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/commented/wall-1010_9020", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        comments: [
                          {
                            comment_id: 9021,
                            post_id: 9020,
                            owner_id: -1010,
                            from_id: 2010,
                            text: 'Comment from commented payload', /* reply text */
                            created_at: '2026-04-07T11:00:00Z',
                          },
                        ],
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/jsonparse/wall-1011_9022", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = JSON.parse("{\"comments\":[{\"comment_id\":9023,\"post_id\":9022,\"owner_id\":-1011,\"from_id\":2011,\"text\":\"Comment from json parse payload\",\"created_at\":\"2026-04-07T13:00:00Z\"}]}");
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/datablob/wall-1012_9024", exchange -> respond(exchange, """
                <html>
                  <body>
                    <div id="replies" data-comments="{&quot;comments&quot;:[{&quot;comment_id&quot;:9025,&quot;post_id&quot;:9024,&quot;owner_id&quot;:-1012,&quot;from_id&quot;:2012,&quot;text&quot;:&quot;Comment from data blob payload&quot;,&quot;created_at&quot;:&quot;2026-04-07T15:00:00Z&quot;}]}"></div>
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
        server.createContext("/id2005", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script id="vk_payload" type="application/json">
                      {
                        "profile": {
                          "id": 2005,
                          "display_name": "Sergey Scriptov",
                          "username": "id2005",
                          "city": "Nakhodka",
                          "home_town": "Partizansk",
                          "birth_date": "05.05.1992",
                          "sex": 2,
                          "status": "script online",
                          "avatar_url": "https://vk.com/images/2005.jpg",
                          "mobile_phone": "+79990000005",
                          "home_phone": "84232000005",
                          "site": "https://sergey.example.com",
                          "education": "DVFU"
                        }
                      }
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/script/id2005", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script id="vk_payload" type="application/json">
                      {
                        "profile": {
                          "id": 2005,
                          "display_name": "Sergey Scriptov",
                          "username": "id2005",
                          "city": "Nakhodka",
                          "home_town": "Partizansk",
                          "birth_date": "05.05.1992",
                          "sex": 2,
                          "status": "script online",
                          "avatar_url": "https://vk.com/images/2005.jpg",
                          "mobile_phone": "+79990000005",
                          "home_phone": "84232000005",
                          "site": "https://sergey.example.com",
                          "education": "DVFU"
                        }
                      }
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/inline/id2006", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "profile": {
                          "id": 2006,
                          "display_name": "Olga Inline",
                          "username": "id2006",
                          "city": "Fokino",
                          "home_town": "Bolshoy Kamen",
                          "birth_date": "06.06.1993",
                          "sex": 1,
                          "status": "inline active",
                          "avatar_url": "https://vk.com/images/2006.jpg",
                          "mobile_phone": "+79990000006",
                          "home_phone": "84232000006",
                          "site": "https://olga.example.com",
                          "education": "MГУ"
                        }
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/nested/id2007", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "state": {
                          "entities": {
                            "profile": {
                              "id": 2007,
                              "display_name": "Nikita Nested",
                              "username": "id2007",
                              "city": "Spassk-Dalny",
                              "home_town": "Lesozavodsk",
                              "birth_date": "07.07.1994",
                              "sex": 2,
                              "status": "nested status",
                              "avatar_url": "https://vk.com/images/2007.jpg",
                              "mobile_phone": "+79990000007",
                              "home_phone": "84232000007",
                              "site": "https://nikita.example.com",
                              "education": "FEFU"
                            }
                          }
                        }
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/mapped/id2008", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        "entities": [
                          {"type": "other"},
                          {
                            "type": "profiles",
                            "items": {
                              "701": {
                                "id": 2008,
                                "display_name": "Maksim Mapped",
                                "username": "id2008",
                                "city": "Dalnerechensk",
                                "home_town": "Kirovsky",
                                "birth_date": "08.08.1995",
                                "sex": 2,
                                "status": "mapped status",
                                "avatar_url": "https://vk.com/images/2008.jpg",
                                "mobile_phone": "+79990000008",
                                "home_phone": "84232000008",
                                "site": "https://maksim.example.com",
                                "education": "VSUES"
                              }
                            }
                          }
                        ]
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/lenient/id2009", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        'profile': {
                          'id': 2009,
                          'display_name': 'Irina Lenient',
                          'username': 'id2009',
                          'city': 'Arsenyev',
                          'home_town': 'Kavalerovo',
                          'birth_date': '09.09.1996',
                          'sex': 1,
                          'status': 'lenient status',
                          'avatar_url': 'https://vk.com/images/2009.jpg',
                          'mobile_phone': '+79990000009',
                          'home_phone': '84232000009',
                          'site': 'https://irina.example.com',
                          'education': 'DVGTU',
                          'relation': undefined,
                        },
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/commented/id2010", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = {
                        profile: {
                          id: 2010,
                          display_name: 'Vera Commented',
                          username: 'id2010',
                          city: 'Ussuriysk',
                          home_town: 'Mikhailovka',
                          birth_date: '10.10.1997',
                          sex: 1,
                          status: 'commented status',
                          avatar_url: 'https://vk.com/images/2010.jpg',
                          mobile_phone: '+79990000010',
                          home_phone: '84232000010',
                          site: 'https://vera.example.com',
                          education: 'VGUES', // education info
                        },
                      };
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/jsonparse/id2011", exchange -> respond(exchange, """
                <html>
                  <body>
                    <script>
                      window.__initialState = JSON.parse("{\"profile\":{\"id\":2011,\"display_name\":\"Roman JsonParse\",\"username\":\"id2011\",\"city\":\"Dalnegorsk\",\"home_town\":\"Kavalerovo\",\"birth_date\":\"11.10.1998\",\"sex\":2,\"status\":\"json parse status\",\"avatar_url\":\"https://vk.com/images/2011.jpg\",\"mobile_phone\":\"+79990000011\",\"home_phone\":\"84232000011\",\"site\":\"https://roman.example.com\",\"education\":\"FEFU\"}}");
                    </script>
                  </body>
                </html>
                """));
        server.createContext("/datablob/id2012", exchange -> respond(exchange, """
                <html>
                  <body>
                    <div id="profile_root" data-profile="{&quot;profile&quot;:{&quot;id&quot;:2012,&quot;display_name&quot;:&quot;Svetlana DataBlob&quot;,&quot;username&quot;:&quot;id2012&quot;,&quot;city&quot;:&quot;Artem&quot;,&quot;home_town&quot;:&quot;Nadezhdinskoye&quot;,&quot;birth_date&quot;:&quot;12.10.1999&quot;,&quot;sex&quot;:1,&quot;status&quot;:&quot;data blob status&quot;,&quot;avatar_url&quot;:&quot;https://vk.com/images/2012.jpg&quot;,&quot;mobile_phone&quot;:&quot;+79990000012&quot;,&quot;home_phone&quot;:&quot;84232000012&quot;,&quot;site&quot;:&quot;https://svetlana.example.com&quot;,&quot;education&quot;:&quot;TGEU&quot;}}"></div>
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

    @Test
    void parsesSearchAndCollectionFromScriptPayloads() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), scriptProperties());

        var groups = client.searchGroups("Script Region", 10);
        var users = client.searchUsers("Script Region", 10);
        var posts = client.getGroupPosts(1003L, 10);
        var comments = client.getPostComments(-1003L, 9009L, 10);
        var profiles = client.getUsersByIds(List.of(2005L));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1003L);
            assertThat(group.name()).isEqualTo("Script Group");
            assertThat(group.screenName()).isEqualTo("club1003");
            assertThat(group.city()).isEqualTo("Nakhodka");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2005L);
            assertThat(user.displayName()).isEqualTo("Sergey Scriptov");
            assertThat(user.username()).isEqualTo("id2005");
            assertThat(user.city()).isEqualTo("Nakhodka");
        });
        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(9009L);
            assertThat(post.authorVkUserId()).isEqualTo(2005L);
            assertThat(post.text()).isEqualTo("Post from script payload");
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(9010L);
            assertThat(comment.authorVkUserId()).isEqualTo(2005L);
            assertThat(comment.text()).isEqualTo("Comment from script payload");
        });
        assertThat(profiles).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Sergey Scriptov");
            assertThat(user.homeTown()).isEqualTo("Partizansk");
            assertThat(user.birthDate()).isEqualTo("05.05.1992");
            assertThat(user.sex()).isEqualTo(2);
            assertThat(user.status()).isEqualTo("script online");
            assertThat(user.site()).isEqualTo("https://sergey.example.com");
            assertThat(user.education()).isEqualTo("DVFU");
        });
    }

    @Test
    void parsesSearchAndCollectionFromInlineScriptPayloads() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), inlineProperties());

        var groups = client.searchGroups("Inline Region", 10);
        var users = client.searchUsers("Inline Region", 10);
        var posts = client.getGroupPosts(1004L, 10);
        var comments = client.getPostComments(-1004L, 9011L, 10);
        var profiles = client.getUsersByIds(List.of(2006L));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1004L);
            assertThat(group.name()).isEqualTo("Inline Group");
            assertThat(group.screenName()).isEqualTo("club1004");
            assertThat(group.city()).isEqualTo("Fokino");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2006L);
            assertThat(user.displayName()).isEqualTo("Olga Inline");
            assertThat(user.username()).isEqualTo("id2006");
            assertThat(user.city()).isEqualTo("Fokino");
        });
        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(9011L);
            assertThat(post.authorVkUserId()).isEqualTo(2006L);
            assertThat(post.text()).isEqualTo("Post from inline script payload");
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(9012L);
            assertThat(comment.authorVkUserId()).isEqualTo(2006L);
            assertThat(comment.text()).isEqualTo("Comment from inline script payload");
        });
        assertThat(profiles).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Olga Inline");
            assertThat(user.homeTown()).isEqualTo("Bolshoy Kamen");
            assertThat(user.birthDate()).isEqualTo("06.06.1993");
            assertThat(user.sex()).isEqualTo(1);
            assertThat(user.status()).isEqualTo("inline active");
            assertThat(user.site()).isEqualTo("https://olga.example.com");
            assertThat(user.education()).isEqualTo("MГУ");
        });
    }

    @Test
    void parsesSearchAndCollectionFromNestedPayloadWrappers() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), nestedProperties());

        var groups = client.searchGroups("Nested Region", 10);
        var users = client.searchUsers("Nested Region", 10);
        var posts = client.getGroupPosts(1005L, 10);
        var comments = client.getPostComments(-1005L, 9013L, 10);
        var profiles = client.getUsersByIds(List.of(2007L));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1005L);
            assertThat(group.name()).isEqualTo("Nested Group");
            assertThat(group.screenName()).isEqualTo("club1005");
            assertThat(group.city()).isEqualTo("Spassk-Dalny");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2007L);
            assertThat(user.displayName()).isEqualTo("Nikita Nested");
            assertThat(user.username()).isEqualTo("id2007");
            assertThat(user.city()).isEqualTo("Spassk-Dalny");
        });
        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(9013L);
            assertThat(post.authorVkUserId()).isEqualTo(2007L);
            assertThat(post.text()).isEqualTo("Post from nested payload");
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(9014L);
            assertThat(comment.authorVkUserId()).isEqualTo(2007L);
            assertThat(comment.text()).isEqualTo("Comment from nested payload");
        });
        assertThat(profiles).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Nikita Nested");
            assertThat(user.homeTown()).isEqualTo("Lesozavodsk");
            assertThat(user.birthDate()).isEqualTo("07.07.1994");
            assertThat(user.sex()).isEqualTo(2);
            assertThat(user.status()).isEqualTo("nested status");
            assertThat(user.site()).isEqualTo("https://nikita.example.com");
            assertThat(user.education()).isEqualTo("FEFU");
        });
    }

    @Test
    void parsesSearchAndCollectionFromMappedPayloadCollections() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), mappedProperties());

        var groups = client.searchGroups("Mapped Region", 10);
        var users = client.searchUsers("Mapped Region", 10);
        var posts = client.getGroupPosts(1006L, 10);
        var comments = client.getPostComments(-1006L, 9015L, 10);
        var profiles = client.getUsersByIds(List.of(2008L));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1006L);
            assertThat(group.name()).isEqualTo("Mapped Group");
            assertThat(group.screenName()).isEqualTo("club1006");
            assertThat(group.city()).isEqualTo("Dalnerechensk");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2008L);
            assertThat(user.displayName()).isEqualTo("Maksim Mapped");
            assertThat(user.username()).isEqualTo("id2008");
            assertThat(user.city()).isEqualTo("Dalnerechensk");
        });
        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(9015L);
            assertThat(post.authorVkUserId()).isEqualTo(2008L);
            assertThat(post.text()).isEqualTo("Post from mapped payload");
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(9016L);
            assertThat(comment.authorVkUserId()).isEqualTo(2008L);
            assertThat(comment.text()).isEqualTo("Comment from mapped payload");
        });
        assertThat(profiles).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Maksim Mapped");
            assertThat(user.homeTown()).isEqualTo("Kirovsky");
            assertThat(user.birthDate()).isEqualTo("08.08.1995");
            assertThat(user.sex()).isEqualTo(2);
            assertThat(user.status()).isEqualTo("mapped status");
            assertThat(user.site()).isEqualTo("https://maksim.example.com");
            assertThat(user.education()).isEqualTo("VSUES");
        });
    }

    @Test
    void parsesSearchAndCollectionFromLenientJsPayloads() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), lenientProperties());

        var groups = client.searchGroups("Lenient Region", 10);
        var users = client.searchUsers("Lenient Region", 10);
        var posts = client.getGroupPosts(1007L, 10);
        var comments = client.getPostComments(-1007L, 9017L, 10);
        var profiles = client.getUsersByIds(List.of(2009L));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1007L);
            assertThat(group.name()).isEqualTo("Lenient Group");
            assertThat(group.screenName()).isEqualTo("club1007");
            assertThat(group.city()).isEqualTo("Arsenyev");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2009L);
            assertThat(user.displayName()).isEqualTo("Irina Lenient");
            assertThat(user.username()).isEqualTo("id2009");
            assertThat(user.city()).isEqualTo("Arsenyev");
        });
        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(9017L);
            assertThat(post.authorVkUserId()).isEqualTo(2009L);
            assertThat(post.text()).isEqualTo("Post from lenient payload");
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(9018L);
            assertThat(comment.authorVkUserId()).isEqualTo(2009L);
            assertThat(comment.text()).isEqualTo("Comment from lenient payload");
        });
        assertThat(profiles).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Irina Lenient");
            assertThat(user.homeTown()).isEqualTo("Kavalerovo");
            assertThat(user.birthDate()).isEqualTo("09.09.1996");
            assertThat(user.sex()).isEqualTo(1);
            assertThat(user.status()).isEqualTo("lenient status");
            assertThat(user.site()).isEqualTo("https://irina.example.com");
            assertThat(user.education()).isEqualTo("DVGTU");
        });
    }

    @Test
    void parsesSearchAndCollectionFromCommentedJsPayloads() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), commentedProperties());

        var groups = client.searchGroups("Commented Region", 10);
        var users = client.searchUsers("Commented Region", 10);
        var posts = client.getGroupPosts(1010L, 10);
        var comments = client.getPostComments(-1010L, 9020L, 10);
        var profiles = client.getUsersByIds(List.of(2010L));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1010L);
            assertThat(group.name()).isEqualTo("Commented Group");
            assertThat(group.screenName()).isEqualTo("club1010");
            assertThat(group.city()).isEqualTo("Ussuriysk");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2010L);
            assertThat(user.displayName()).isEqualTo("Vera Commented");
            assertThat(user.username()).isEqualTo("id2010");
            assertThat(user.city()).isEqualTo("Ussuriysk");
        });
        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(9020L);
            assertThat(post.authorVkUserId()).isEqualTo(2010L);
            assertThat(post.text()).isEqualTo("Post from commented payload");
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(9021L);
            assertThat(comment.authorVkUserId()).isEqualTo(2010L);
            assertThat(comment.text()).isEqualTo("Comment from commented payload");
        });
        assertThat(profiles).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Vera Commented");
            assertThat(user.homeTown()).isEqualTo("Mikhailovka");
            assertThat(user.birthDate()).isEqualTo("10.10.1997");
            assertThat(user.sex()).isEqualTo(1);
            assertThat(user.status()).isEqualTo("commented status");
            assertThat(user.site()).isEqualTo("https://vera.example.com");
            assertThat(user.education()).isEqualTo("VGUES");
        });
    }

    @Test
    void parsesSearchAndCollectionFromJsonParsePayloads() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), jsonParseProperties());

        var groups = client.searchGroups("JsonParse Region", 10);
        var users = client.searchUsers("JsonParse Region", 10);
        var posts = client.getGroupPosts(1011L, 10);
        var comments = client.getPostComments(-1011L, 9022L, 10);
        var profiles = client.getUsersByIds(List.of(2011L));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1011L);
            assertThat(group.name()).isEqualTo("JsonParse Group");
            assertThat(group.screenName()).isEqualTo("club1011");
            assertThat(group.city()).isEqualTo("Dalnegorsk");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2011L);
            assertThat(user.displayName()).isEqualTo("Roman JsonParse");
            assertThat(user.username()).isEqualTo("id2011");
            assertThat(user.city()).isEqualTo("Dalnegorsk");
        });
        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(9022L);
            assertThat(post.authorVkUserId()).isEqualTo(2011L);
            assertThat(post.text()).isEqualTo("Post from json parse payload");
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(9023L);
            assertThat(comment.authorVkUserId()).isEqualTo(2011L);
            assertThat(comment.text()).isEqualTo("Comment from json parse payload");
        });
        assertThat(profiles).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Roman JsonParse");
            assertThat(user.homeTown()).isEqualTo("Kavalerovo");
            assertThat(user.birthDate()).isEqualTo("11.10.1998");
            assertThat(user.sex()).isEqualTo(2);
            assertThat(user.status()).isEqualTo("json parse status");
            assertThat(user.site()).isEqualTo("https://roman.example.com");
            assertThat(user.education()).isEqualTo("FEFU");
        });
    }

    @Test
    void parsesSearchAndCollectionFromHtmlDataBlobs() {
        HttpVkFallbackClient client = new HttpVkFallbackClient(RestClient.builder(), new ObjectMapper(), dataBlobProperties());

        var groups = client.searchGroups("DataBlob Region", 10);
        var users = client.searchUsers("DataBlob Region", 10);
        var posts = client.getGroupPosts(1012L, 10);
        var comments = client.getPostComments(-1012L, 9024L, 10);
        var profiles = client.getUsersByIds(List.of(2012L));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo(1012L);
            assertThat(group.name()).isEqualTo("DataBlob Group");
            assertThat(group.screenName()).isEqualTo("club1012");
            assertThat(group.city()).isEqualTo("Artem");
        });
        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(2012L);
            assertThat(user.displayName()).isEqualTo("Svetlana DataBlob");
            assertThat(user.username()).isEqualTo("id2012");
            assertThat(user.city()).isEqualTo("Artem");
        });
        assertThat(posts).singleElement().satisfies(post -> {
            assertThat(post.postId()).isEqualTo(9024L);
            assertThat(post.authorVkUserId()).isEqualTo(2012L);
            assertThat(post.text()).isEqualTo("Post from data blob payload");
        });
        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.commentId()).isEqualTo(9025L);
            assertThat(comment.authorVkUserId()).isEqualTo(2012L);
            assertThat(comment.text()).isEqualTo("Comment from data blob payload");
        });
        assertThat(profiles).singleElement().satisfies(user -> {
            assertThat(user.displayName()).isEqualTo("Svetlana DataBlob");
            assertThat(user.homeTown()).isEqualTo("Nadezhdinskoye");
            assertThat(user.birthDate()).isEqualTo("12.10.1999");
            assertThat(user.sex()).isEqualTo(1);
            assertThat(user.status()).isEqualTo("data blob status");
            assertThat(user.site()).isEqualTo("https://svetlana.example.com");
            assertThat(user.education()).isEqualTo("TGEU");
        });
    }

    private VkProperties properties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl,
                Duration.ofSeconds(5)
        );
    }

    private VkProperties scriptProperties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl + "/script",
                Duration.ofSeconds(5)
        );
    }

    private VkProperties inlineProperties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl + "/inline",
                Duration.ofSeconds(5)
        );
    }

    private VkProperties nestedProperties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl + "/nested",
                Duration.ofSeconds(5)
        );
    }

    private VkProperties mappedProperties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl + "/mapped",
                Duration.ofSeconds(5)
        );
    }

    private VkProperties lenientProperties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl + "/lenient",
                Duration.ofSeconds(5)
        );
    }

    private VkProperties commentedProperties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl + "/commented",
                Duration.ofSeconds(5)
        );
    }

    private VkProperties jsonParseProperties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl + "/jsonparse",
                Duration.ofSeconds(5)
        );
    }

    private VkProperties dataBlobProperties() {
        return new VkProperties(
                42L,
                "vk-token",
                "vk-user-token",
                "5.199",
                "https://api.vk.com/method",
                baseUrl + "/datablob",
                Duration.ofSeconds(5)
        );
    }

    private void respondSearch(HttpExchange exchange) throws IOException {
        String query = firstNonBlank(exchange.getRequestURI().getQuery(), exchange.getRequestURI().getRawQuery());
        if (exchange.getRequestURI().getPath().startsWith("/script")) {
            respond(exchange, """
                    <html>
                      <body>
                        <script id="vk_payload" type="application/json">
                          {
                            "groups": [
                              {
                                "id": 1003,
                                "name": "Script Group",
                                "screen_name": "club1003",
                                "description": "Search group from script payload",
                                "city": "Nakhodka"
                              }
                            ],
                            "users": [
                              {
                                "id": 2005,
                                "display_name": "Sergey Scriptov",
                                "first_name": "Sergey",
                                "last_name": "Scriptov",
                                "username": "id2005",
                                "city": "Nakhodka",
                                "home_town": "Partizansk",
                                "avatar_url": "https://vk.com/images/2005.jpg"
                              }
                            ]
                          }
                        </script>
                      </body>
                    </html>
                    """);
            return;
        }
        if (exchange.getRequestURI().getPath().startsWith("/inline")) {
            respond(exchange, """
                    <html>
                      <body>
                        <script>
                          window.__initialState = {
                            "groups": [
                              {
                                "id": 1004,
                                "name": "Inline Group",
                                "screen_name": "club1004",
                                "description": "Search group from inline script payload",
                                "city": "Fokino"
                              }
                            ],
                            "users": [
                              {
                                "id": 2006,
                                "display_name": "Olga Inline",
                                "first_name": "Olga",
                                "last_name": "Inline",
                                "username": "id2006",
                                "city": "Fokino",
                                "home_town": "Bolshoy Kamen",
                                "avatar_url": "https://vk.com/images/2006.jpg"
                              }
                            ]
                          };
                        </script>
                      </body>
                    </html>
                    """);
            return;
        }
        if (exchange.getRequestURI().getPath().startsWith("/nested")) {
            respond(exchange, """
                    <html>
                      <body>
                        <script>
                          window.__initialState = {
                            "payload": {
                              "search": {
                                "result": {
                                  "items": {
                                    "groups": [
                                      {
                                        "id": 1005,
                                        "name": "Nested Group",
                                        "screen_name": "club1005",
                                        "description": "Search group from nested payload",
                                        "city": "Spassk-Dalny"
                                      }
                                    ],
                                    "users": [
                                      {
                                        "id": 2007,
                                        "display_name": "Nikita Nested",
                                        "first_name": "Nikita",
                                        "last_name": "Nested",
                                        "username": "id2007",
                                        "city": "Spassk-Dalny",
                                        "home_town": "Lesozavodsk",
                                        "avatar_url": "https://vk.com/images/2007.jpg"
                                      }
                                    ]
                                  }
                                }
                              }
                            }
                          };
                        </script>
                      </body>
                    </html>
                    """);
            return;
        }
        if (exchange.getRequestURI().getPath().startsWith("/mapped")) {
            respond(exchange, """
                    <html>
                      <body>
                        <script>
                          window.__initialState = {
                            "search": {
                              "events": [
                                {"kind": "summary"},
                                {
                                  "kind": "groups",
                                  "items": {
                                    "801": {
                                      "id": 1006,
                                      "name": "Mapped Group",
                                      "screen_name": "club1006",
                                      "description": "Search group from mapped payload",
                                      "city": "Dalnerechensk"
                                    }
                                  }
                                },
                                {
                                  "kind": "users",
                                  "items": {
                                    "901": {
                                      "id": 2008,
                                      "display_name": "Maksim Mapped",
                                      "first_name": "Maksim",
                                      "last_name": "Mapped",
                                      "username": "id2008",
                                      "city": "Dalnerechensk",
                                      "home_town": "Kirovsky",
                                      "avatar_url": "https://vk.com/images/2008.jpg"
                                    }
                                  }
                                }
                              ]
                            }
                          };
                        </script>
                      </body>
                    </html>
                    """);
            return;
        }
        if (exchange.getRequestURI().getPath().startsWith("/lenient")) {
            respond(exchange, """
                    <html>
                      <body>
                        <script>
                          window.__initialState = {
                            'groups': [
                              {
                                'id': 1007,
                                'name': 'Lenient Group',
                                'screen_name': 'club1007',
                                'description': 'Search group from lenient payload',
                                'city': 'Arsenyev',
                                'extra': undefined,
                              },
                            ],
                            'users': [
                              {
                                'id': 2009,
                                'display_name': 'Irina Lenient',
                                'first_name': 'Irina',
                                'last_name': 'Lenient',
                                'username': 'id2009',
                                'city': 'Arsenyev',
                                'home_town': 'Kavalerovo',
                                'avatar_url': 'https://vk.com/images/2009.jpg',
                              },
                            ],
                          };
                        </script>
                      </body>
                    </html>
                    """);
            return;
        }
        if (exchange.getRequestURI().getPath().startsWith("/commented")) {
            respond(exchange, """
                    <html>
                      <body>
                        <script>
                          window.__initialState = {
                            groups: [
                              {
                                id: 1010,
                                name: 'Commented Group',
                                screen_name: 'club1010',
                                description: 'Search group from commented payload', // group description
                                city: 'Ussuriysk',
                              },
                            ],
                            users: [
                              {
                                id: 2010,
                                display_name: 'Vera Commented',
                                first_name: 'Vera',
                                last_name: 'Commented',
                                username: 'id2010',
                                city: 'Ussuriysk',
                                home_town: 'Mikhailovka',
                                avatar_url: 'https://vk.com/images/2010.jpg', /* avatar */
                              },
                            ],
                          };
                        </script>
                      </body>
                    </html>
                    """);
            return;
        }
        if (exchange.getRequestURI().getPath().startsWith("/jsonparse")) {
            respond(exchange, """
                    <html>
                      <body>
                        <script>
                          window.__initialState = JSON.parse("{\"groups\":[{\"id\":1011,\"name\":\"JsonParse Group\",\"screen_name\":\"club1011\",\"description\":\"Search group from json parse payload\",\"city\":\"Dalnegorsk\"}],\"users\":[{\"id\":2011,\"display_name\":\"Roman JsonParse\",\"first_name\":\"Roman\",\"last_name\":\"JsonParse\",\"username\":\"id2011\",\"city\":\"Dalnegorsk\",\"home_town\":\"Kavalerovo\",\"avatar_url\":\"https://vk.com/images/2011.jpg\"}]}");
                        </script>
                      </body>
                    </html>
                    """);
            return;
        }
        if (exchange.getRequestURI().getPath().startsWith("/datablob")) {
            respond(exchange, """
                    <html>
                      <body>
                        <div id="search_root" data-store="{&quot;groups&quot;:[{&quot;id&quot;:1012,&quot;name&quot;:&quot;DataBlob Group&quot;,&quot;screen_name&quot;:&quot;club1012&quot;,&quot;description&quot;:&quot;Search group from data blob payload&quot;,&quot;city&quot;:&quot;Artem&quot;}],&quot;users&quot;:[{&quot;id&quot;:2012,&quot;display_name&quot;:&quot;Svetlana DataBlob&quot;,&quot;first_name&quot;:&quot;Svetlana&quot;,&quot;last_name&quot;:&quot;DataBlob&quot;,&quot;username&quot;:&quot;id2012&quot;,&quot;city&quot;:&quot;Artem&quot;,&quot;home_town&quot;:&quot;Nadezhdinskoye&quot;,&quot;avatar_url&quot;:&quot;https://vk.com/images/2012.jpg&quot;}]}"></div>
                      </body>
                    </html>
                    """);
            return;
        }
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
