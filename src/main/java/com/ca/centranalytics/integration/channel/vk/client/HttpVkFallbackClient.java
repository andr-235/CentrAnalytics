package com.ca.centranalytics.integration.channel.vk.client;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HttpVkFallbackClient implements VkFallbackClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpVkFallbackClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, VkProperties vkProperties) {
        this.restClient = restClientBuilder
                .baseUrl(normalizeBaseUrl(vkProperties.fallbackBaseUrl()))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<VkGroupSearchResult> searchGroups(String region, int limit) {
        Document document = getDocument("/search", Map.of(
                "c[section]", "communities",
                "c[q]", region
        ));
        List<VkGroupSearchResult> results = new ArrayList<>();
        for (Element row : document.select(".groups_row, .group_row")) {
            if (results.size() >= limit) {
                break;
            }
            String href = extractHref(row, ".groups_row_title, .group_row_title, a[href]");
            Long groupId = parseGroupId(href);
            if (groupId == null) {
                continue;
            }
            results.add(new VkGroupSearchResult(
                    groupId,
                    text(row, ".groups_row_title, .group_row_title, a[href]"),
                    normalizeScreenName(href),
                    text(row, ".groups_row_description, .group_row_description, .groups_row_info"),
                    text(row, ".groups_row_city, .group_row_city"),
                    rawJson(rawPayload(
                            "id", groupId,
                            "href", href,
                            "name", text(row, ".groups_row_title, .group_row_title, a[href]")
                    ))
            ));
        }
        return results;
    }

    @Override
    public List<VkUserSearchResult> searchUsers(String region, int limit) {
        Document document = getDocument("/search", Map.of(
                "c[section]", "people",
                "c[q]", region
        ));
        List<VkUserSearchResult> results = new ArrayList<>();
        for (Element row : document.select(".people_row, .search_row")) {
            if (results.size() >= limit) {
                break;
            }
            String href = firstNonBlank(
                    extractHref(row, ".people_row_name, .search_row_name"),
                    extractHref(row, "a[href^=/id]"),
                    extractHref(row, ".people_row_photo")
            );
            Long userId = parseUserId(href);
            if (userId == null) {
                continue;
            }
            String displayName = firstNonBlank(
                    text(row, ".people_row_name, .search_row_name"),
                    text(row, "a[href^=/id]")
            );
            String[] names = splitName(displayName);
            results.add(new VkUserSearchResult(
                    userId,
                    displayName,
                    names[0],
                    names[1],
                    normalizeScreenName(href),
                    profileUrl(href),
                    text(row, ".people_row_city, .search_row_city"),
                    text(row, ".people_row_details, .search_row_details"),
                    null,
                    null,
                    null,
                    null,
                    attr(row, ".people_row_photo img, img", "src"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    rawJson(rawPayload(
                            "id", userId,
                            "href", href,
                            "name", displayName
                    ))
            ));
        }
        return results;
    }

    @Override
    public List<VkWallPostResult> getGroupPosts(Long groupId, int limit) {
        Document document = getDocument("/public" + groupId, Map.of());
        List<VkWallPostResult> results = new ArrayList<>();
        for (Element post : document.select(".wall_post, .post")) {
            if (results.size() >= limit) {
                break;
            }
            Long postId = parsePostId(post.attr("data-post-id"));
            if (postId == null) {
                continue;
            }
            results.add(new VkWallPostResult(
                    parseOwnerId(post.attr("data-post-id"), groupId),
                    postId,
                    parseLong(post.attr("data-from-id")),
                    text(post, ".wall_post_text, .post_text, .pi_text"),
                    parseTime(post.selectFirst("time")),
                    rawJson(rawPayload(
                            "postId", postId,
                            "ownerId", parseOwnerId(post.attr("data-post-id"), groupId)
                    ))
            ));
        }
        return results;
    }

    @Override
    public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
        Document document = getDocument("/wall" + ownerId + "_" + postId, Map.of("reply", "all"));
        List<VkCommentResult> results = new ArrayList<>();
        for (Element reply : document.select(".reply, .wall_reply")) {
            if (results.size() >= limit) {
                break;
            }
            Long commentId = parseReplyId(reply.id(), reply.attr("data-reply-id"));
            if (commentId == null) {
                continue;
            }
            String href = extractHref(reply, ".reply_author, .wall_reply_author, a[href^=/id]");
            results.add(new VkCommentResult(
                    ownerId,
                    postId,
                    commentId,
                    parseUserId(href),
                    text(reply, ".reply_text, .wall_reply_text"),
                    parseTime(reply.selectFirst("time")),
                    rawJson(rawPayload(
                            "commentId", commentId,
                            "authorHref", href
                    ))
            ));
        }
        return results;
    }

    @Override
    public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
        List<VkUserSearchResult> results = new ArrayList<>();
        for (Long userId : userIds) {
            Document document = getDocument("/id" + userId, Map.of());
            String displayName = firstNonBlank(
                    text(document, ".page_name, h1"),
                    extractJsonLdValue(document, "name")
            );
            String username = firstNonBlank(
                    extractJsonLdValue(document, "alternateName"),
                    "id" + userId
            );
            String[] names = splitName(displayName);
            results.add(new VkUserSearchResult(
                    userId,
                    displayName,
                    names[0],
                    names[1],
                    username,
                    "https://vk.com/" + username,
                    profileField(document, "Город:"),
                    profileField(document, "Родной город:"),
                    profileField(document, "Дата рождения:"),
                    null,
                    text(document, ".profile_status"),
                    null,
                    firstNonBlank(attr(document, ".page_avatar_img, img.page_avatar_img", "src"), extractJsonLdValue(document, "image")),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    rawJson(rawPayload(
                            "id", userId,
                            "username", username,
                            "displayName", displayName
                    ))
            ));
        }
        return results;
    }

    private Document getDocument(String path, Map<String, String> params) {
        String body = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(path);
                    params.forEach(builder::queryParam);
                    return builder.build();
                })
                .retrieve()
                .body(String.class);
        if (!StringUtils.hasText(body)) {
            throw new IllegalStateException("VK fallback returned empty body for " + path);
        }
        return Jsoup.parse(body);
    }

    private String profileField(Document document, String label) {
        for (Element row : document.select(".profile_info_row")) {
            String currentLabel = text(row, ".label");
            if (label.equals(currentLabel)) {
                return text(row, ".value");
            }
        }
        return null;
    }

    private String extractJsonLdValue(Document document, String field) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                String value = objectMapper.readTree(script.data()).path(field).asText(null);
                if (StringUtils.hasText(value)) {
                    return value;
                }
            } catch (JsonProcessingException ignored) {
            }
        }
        return null;
    }

    private String text(Element parent, String selector) {
        Element element = parent.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String value = element.text();
        return StringUtils.hasText(value) ? value : null;
    }

    private String attr(Element parent, String selector, String attrName) {
        Element element = parent.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String value = element.attr(attrName);
        return StringUtils.hasText(value) ? value : null;
    }

    private String extractHref(Element parent, String selector) {
        return attr(parent, selector, "href");
    }

    private String normalizeScreenName(String href) {
        if (!StringUtils.hasText(href)) {
            return null;
        }
        String normalized = href.startsWith("/") ? href.substring(1) : href;
        int queryIndex = normalized.indexOf('?');
        return queryIndex >= 0 ? normalized.substring(0, queryIndex) : normalized;
    }

    private String profileUrl(String href) {
        String screenName = normalizeScreenName(href);
        return screenName == null ? null : "https://vk.com/" + screenName;
    }

    private String[] splitName(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return new String[]{null, null};
        }
        String[] parts = displayName.trim().split("\\s+", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : null};
    }

    private Long parseGroupId(String href) {
        String screenName = normalizeScreenName(href);
        if (!StringUtils.hasText(screenName)) {
            return null;
        }
        if (screenName.startsWith("public")) {
            return parseLong(screenName.substring("public".length()));
        }
        if (screenName.startsWith("club")) {
            return parseLong(screenName.substring("club".length()));
        }
        return null;
    }

    private Long parseUserId(String href) {
        String screenName = normalizeScreenName(href);
        if (!StringUtils.hasText(screenName)) {
            return null;
        }
        if (screenName.startsWith("id")) {
            return parseLong(screenName.substring(2));
        }
        return null;
    }

    private Long parsePostId(String rawPostId) {
        if (!StringUtils.hasText(rawPostId)) {
            return null;
        }
        int separator = rawPostId.indexOf('_');
        if (separator < 0) {
            return null;
        }
        return parseLong(rawPostId.substring(separator + 1));
    }

    private Long parseOwnerId(String rawPostId, Long groupId) {
        if (!StringUtils.hasText(rawPostId)) {
            return -Math.abs(groupId);
        }
        int separator = rawPostId.indexOf('_');
        if (separator < 0) {
            return -Math.abs(groupId);
        }
        return parseLong(rawPostId.substring(0, separator));
    }

    private Long parseReplyId(String idAttr, String dataReplyId) {
        Long explicit = parseLong(dataReplyId);
        if (explicit != null) {
            return explicit;
        }
        if (!StringUtils.hasText(idAttr)) {
            return null;
        }
        return parseLong(idAttr.replace("reply", ""));
    }

    private Instant parseTime(Element element) {
        if (element == null) {
            return null;
        }
        String dateTime = element.attr("datetime");
        if (StringUtils.hasText(dateTime)) {
            return OffsetDateTime.parse(dateTime).toInstant();
        }
        return null;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String rawJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize VK fallback payload", ex);
        }
    }

    private Map<String, Object> rawPayload(Object... keyValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            payload.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return payload;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://vk.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
