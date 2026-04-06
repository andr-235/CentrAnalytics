package com.ca.centranalytics.integration.channel.vk.client;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
public class HttpVkOfficialClient implements VkOfficialClient {

    private static final String DEFAULT_API_BASE_URL = "https://api.vk.com/method";
    private static final String DEFAULT_API_VERSION = "5.199";

    private final RestClient restClient;
    private final VkProperties vkProperties;
    private final ObjectMapper objectMapper;

    public HttpVkOfficialClient(RestClient.Builder restClientBuilder, VkProperties vkProperties, ObjectMapper objectMapper) {
        this.vkProperties = vkProperties;
        this.objectMapper = objectMapper;
        java.time.Duration requestTimeout = vkProperties.requestTimeout() == null ? java.time.Duration.ofSeconds(5) : vkProperties.requestTimeout();
        String apiBaseUrl = StringUtils.hasText(vkProperties.apiBaseUrl()) ? vkProperties.apiBaseUrl() : DEFAULT_API_BASE_URL;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(requestTimeout)
                        .build()
        );
        requestFactory.setReadTimeout(requestTimeout);

        this.restClient = restClientBuilder
                .baseUrl(apiBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<VkGroupSearchResult> searchGroups(String region, int limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("q", region);
        params.put("type", "group");
        params.put("count", limit);
        params.put("fields", "description,city");

        JsonNode items = invokeItemsMethod("groups.search", officialAccessToken(), params);

        return StreamSupport.stream(items.spliterator(), false)
                .map(item -> new VkGroupSearchResult(
                        item.path("id").asLong(),
                        text(item, "name"),
                        text(item, "screen_name"),
                        text(item, "description"),
                        nestedText(item, "city", "title"),
                        writeValue(item)
                ))
                .toList();
    }

    @Override
    public List<VkUserSearchResult> searchUsers(String region, int limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("q", region);
        params.put("count", limit);
        params.put("fields", userFields());

        JsonNode items = invokeItemsMethod("users.search", userAccessToken(), params);
        return StreamSupport.stream(items.spliterator(), false)
                .map(this::toUserResult)
                .toList();
    }

    @Override
    public List<VkWallPostResult> getGroupPosts(Long groupId, int limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("owner_id", -Math.abs(groupId));
        params.put("count", limit);

        JsonNode items = invokeItemsMethod("wall.get", officialAccessToken(), params);
        return StreamSupport.stream(items.spliterator(), false)
                .map(item -> new VkWallPostResult(
                        item.path("owner_id").asLong(),
                        item.path("id").asLong(),
                        positiveOrNull(item.path("from_id").asLong()),
                        text(item, "text"),
                        parseUnixTimestamp(item.path("date")),
                        writeValue(item)
                ))
                .toList();
    }

    @Override
    public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("owner_id", ownerId);
        params.put("post_id", postId);
        params.put("count", limit);
        params.put("thread_items_count", 0);

        JsonNode items = invokeItemsMethod("wall.getComments", officialAccessToken(), params);
        return StreamSupport.stream(items.spliterator(), false)
                .map(item -> new VkCommentResult(
                        ownerId,
                        postId,
                        item.path("id").asLong(),
                        positiveOrNull(item.path("from_id").asLong()),
                        text(item, "text"),
                        parseUnixTimestamp(item.path("date")),
                        writeValue(item)
                ))
                .toList();
    }

    @Override
    public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("user_ids", userIds.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse(""));
        params.put("fields", userFields());

        JsonNode response = invokeMethod("users.get", userAccessToken(), params);
        return StreamSupport.stream(response.spliterator(), false)
                .map(this::toUserResult)
                .toList();
    }

    private VkUserSearchResult toUserResult(JsonNode item) {
        long userId = item.path("id").asLong();
        String firstName = text(item, "first_name");
        String lastName = text(item, "last_name");
        String screenName = firstNonBlank(text(item, "screen_name"), text(item, "domain"));
        String profileUrl = "https://vk.com/" + firstNonBlank(screenName, "id" + userId);
        String displayName = firstNonBlank(
                text(item, "display_name"),
                joinNonBlank(firstName, lastName)
        );
        String city = firstNonBlank(nestedText(item, "city", "title"), text(item, "home_town"));

        return new VkUserSearchResult(
                userId,
                displayName,
                firstName,
                lastName,
                profileUrl,
                city,
                writeValue(item)
        );
    }

    private JsonNode invokeItemsMethod(String methodName, String accessToken, Map<String, Object> params) {
        JsonNode response = invokeMethod(methodName, accessToken, params);
        return response.path("items").isArray() ? response.path("items") : objectMapper.createArrayNode();
    }

    private JsonNode invokeMethod(String methodName, String accessToken, Map<String, Object> params) {
        String body = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.pathSegment(methodName)
                            .queryParam("access_token", accessToken)
                            .queryParam("v", apiVersion());
                    params.forEach(builder::queryParam);
                    return builder.build();
                })
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(body)) {
            throw new IllegalStateException("VK API returned empty body for " + methodName);
        }
        JsonNode root = readTree(body);
        if (root.has("error")) {
            throw new IllegalStateException("VK API error for " + methodName + ": " + writeValue(root.path("error")));
        }
        return root.path("response");
    }

    private String officialAccessToken() {
        if (!StringUtils.hasText(vkProperties.accessToken())) {
            throw new IllegalStateException("integration.vk.access-token is required for VK API calls");
        }
        return vkProperties.accessToken();
    }

    private String userAccessToken() {
        return StringUtils.hasText(vkProperties.userAccessToken()) ? vkProperties.userAccessToken() : officialAccessToken();
    }

    private String userFields() {
        return "city,home_town,screen_name,domain,bdate,sex,status,last_seen,photo_200,contacts,connections,career,education,relation,counters";
    }

    private String apiVersion() {
        return StringUtils.hasText(vkProperties.apiVersion()) ? vkProperties.apiVersion() : DEFAULT_API_VERSION;
    }

    private String text(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private String nestedText(JsonNode node, String fieldName, String nestedFieldName) {
        JsonNode nestedNode = node.path(fieldName);
        if (nestedNode.isMissingNode() || nestedNode.isNull()) {
            return null;
        }
        return text(nestedNode, nestedFieldName);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String joinNonBlank(String... values) {
        return String.join(" ", java.util.Arrays.stream(values)
                .filter(StringUtils::hasText)
                .toList());
    }

    private Instant parseUnixTimestamp(JsonNode node) {
        return node.isNumber() ? Instant.ofEpochSecond(node.asLong()) : null;
    }

    private Long positiveOrNull(long value) {
        return value > 0 ? value : null;
    }

    private String writeValue(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize VK response payload", ex);
        }
    }

    private JsonNode readTree(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse VK response payload", ex);
        }
    }
}
