package com.ca.centranalytics.integration.channel.vk.client;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HttpVkFallbackClient implements VkFallbackClient {

    private static final Pattern SINGLE_QUOTED_STRING = Pattern.compile("'((?:\\\\.|[^'\\\\])*)'");
    private static final Pattern TRAILING_COMMA = Pattern.compile(",(?=\\s*[}\\]])");
    private static final Pattern UNDEFINED_LITERAL = Pattern.compile("(?<![\\w$])undefined(?![\\w$])");

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
        if (!results.isEmpty()) {
            return results;
        }
        for (JsonNode payload : scriptPayloads(document)) {
            JsonNode groupsNode = findArrayField(payload, "groups");
            if (groupsNode == null) {
                continue;
            }
            for (JsonNode group : groupsNode) {
                if (results.size() >= limit) {
                    break;
                }
                Long groupId = longValue(group, "id");
                if (groupId == null) {
                    continue;
                }
                results.add(new VkGroupSearchResult(
                        groupId,
                        textValue(group, "name"),
                        textValue(group, "screen_name"),
                        textValue(group, "description"),
                        textValue(group, "city"),
                        rawJson(group)
                ));
            }
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
        if (!results.isEmpty()) {
            return results;
        }
        for (JsonNode payload : scriptPayloads(document)) {
            JsonNode usersNode = findArrayField(payload, "users");
            if (usersNode == null) {
                continue;
            }
            for (JsonNode user : usersNode) {
                if (results.size() >= limit) {
                    break;
                }
                Long userId = longValue(user, "id");
                if (userId == null) {
                    continue;
                }
                String displayName = textValue(user, "display_name");
                String[] names = splitName(displayName);
                String username = firstNonBlank(textValue(user, "username"), textValue(user, "screen_name"), "id" + userId);
                results.add(new VkUserSearchResult(
                        userId,
                        displayName,
                        firstNonBlank(textValue(user, "first_name"), names[0]),
                        firstNonBlank(textValue(user, "last_name"), names[1]),
                        username,
                        "https://vk.com/" + username,
                        textValue(user, "city"),
                        textValue(user, "home_town"),
                        null,
                        integerValue(user, "sex"),
                        textValue(user, "status"),
                        null,
                        textValue(user, "avatar_url"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        rawJson(user)
                ));
            }
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
            String authorHref = firstNonBlank(
                    extractHref(post, ".post_author, .wall_post_author"),
                    extractHref(post, "a[href^=/id]")
            );
            results.add(new VkWallPostResult(
                    parseOwnerId(post.attr("data-post-id"), groupId),
                    postId,
                    firstNonNull(
                            parseLong(post.attr("data-from-id")),
                            parseUserId(authorHref)
                    ),
                    text(post, ".wall_post_text, .post_text, .pi_text"),
                    parseTime(firstNonNullElement(post.selectFirst("time"), post.selectFirst("[data-time]"))),
                    rawJson(rawPayload(
                            "postId", postId,
                            "ownerId", parseOwnerId(post.attr("data-post-id"), groupId),
                            "authorHref", authorHref
                    ))
            ));
        }
        if (!results.isEmpty()) {
            return results;
        }
        for (JsonNode payload : scriptPayloads(document)) {
            JsonNode postsNode = findArrayField(payload, "posts");
            if (postsNode == null) {
                continue;
            }
            for (JsonNode post : postsNode) {
                if (results.size() >= limit) {
                    break;
                }
                Long postId = longValue(post, "post_id");
                if (postId == null) {
                    continue;
                }
                results.add(new VkWallPostResult(
                        firstNonNull(longValue(post, "owner_id"), -Math.abs(groupId)),
                        postId,
                        longValue(post, "from_id"),
                        textValue(post, "text"),
                        parseInstant(textValue(post, "created_at")),
                        rawJson(post)
                ));
            }
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
                    parseTime(firstNonNullElement(reply.selectFirst("time"), reply.selectFirst("[data-time]"))),
                    rawJson(rawPayload(
                            "commentId", commentId,
                            "authorHref", href
                    ))
            ));
        }
        if (!results.isEmpty()) {
            return results;
        }
        for (JsonNode payload : scriptPayloads(document)) {
            JsonNode commentsNode = findArrayField(payload, "comments");
            if (commentsNode == null) {
                continue;
            }
            for (JsonNode comment : commentsNode) {
                if (results.size() >= limit) {
                    break;
                }
                Long commentId = longValue(comment, "comment_id");
                if (commentId == null) {
                    continue;
                }
                results.add(new VkCommentResult(
                        firstNonNull(longValue(comment, "owner_id"), ownerId),
                        firstNonNull(longValue(comment, "post_id"), postId),
                        commentId,
                        longValue(comment, "from_id"),
                        textValue(comment, "text"),
                        parseInstant(textValue(comment, "created_at")),
                        rawJson(comment)
                ));
            }
        }
        return results;
    }

    @Override
    public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
        List<VkUserSearchResult> results = new ArrayList<>();
        for (Long userId : userIds) {
            Document document = getDocument("/id" + userId, Map.of());
            VkUserSearchResult htmlResult = htmlProfileResult(document, userId);
            if (hasMeaningfulProfileData(htmlResult)) {
                results.add(htmlResult);
                continue;
            }
            VkUserSearchResult scriptResult = scriptProfileResult(document, userId);
            if (scriptResult != null) {
                results.add(scriptResult);
            } else {
                results.add(htmlResult);
            }
        }
        return results;
    }

    private VkUserSearchResult htmlProfileResult(Document document, Long userId) {
        String displayName = firstNonBlank(
                text(document, ".page_name, h1"),
                extractJsonLdValue(document, "name")
        );
        String username = firstNonBlank(
                extractJsonLdValue(document, "alternateName"),
                "id" + userId
        );
        String[] names = splitName(displayName);
        return new VkUserSearchResult(
                userId,
                displayName,
                names[0],
                names[1],
                username,
                "https://vk.com/" + username,
                profileField(document, "Город:"),
                profileField(document, "Родной город:"),
                profileField(document, "Дата рождения:"),
                parseSex(profileField(document, "Пол:")),
                text(document, ".profile_status"),
                null,
                firstNonBlank(
                        attr(document, ".page_avatar_img, img.page_avatar_img", "src"),
                        attr(document, "meta[property=og:image]", "content"),
                        extractJsonLdValue(document, "image")
                ),
                profileField(document, "Моб. телефон:"),
                profileField(document, "Дом. телефон:"),
                profileField(document, "Сайт:"),
                null,
                profileField(document, "Образование:"),
                null,
                null,
                rawJson(rawPayload(
                        "id", userId,
                        "username", username,
                        "displayName", displayName
                ))
        );
    }

    private boolean hasMeaningfulProfileData(VkUserSearchResult result) {
        return StringUtils.hasText(result.displayName())
                || StringUtils.hasText(result.city())
                || StringUtils.hasText(result.homeTown())
                || StringUtils.hasText(result.birthDate())
                || result.sex() != null
                || StringUtils.hasText(result.status())
                || StringUtils.hasText(result.avatarUrl())
                || StringUtils.hasText(result.mobilePhone())
                || StringUtils.hasText(result.homePhone())
                || StringUtils.hasText(result.site())
                || result.relation() != null
                || StringUtils.hasText(result.education())
                || StringUtils.hasText(result.careerJson())
                || StringUtils.hasText(result.countersJson());
    }

    private VkUserSearchResult scriptProfileResult(Document document, Long userId) {
        for (JsonNode payload : scriptPayloads(document)) {
            JsonNode profile = findObjectField(payload, "profile");
            if (profile == null) {
                continue;
            }
            String displayName = textValue(profile, "display_name");
            String[] names = splitName(displayName);
            String username = firstNonBlank(textValue(profile, "username"), "id" + userId);
            return new VkUserSearchResult(
                    firstNonNull(longValue(profile, "id"), userId),
                    displayName,
                    firstNonBlank(textValue(profile, "first_name"), names[0]),
                    firstNonBlank(textValue(profile, "last_name"), names[1]),
                    username,
                    "https://vk.com/" + username,
                    textValue(profile, "city"),
                    textValue(profile, "home_town"),
                    textValue(profile, "birth_date"),
                    integerValue(profile, "sex"),
                    textValue(profile, "status"),
                    null,
                    textValue(profile, "avatar_url"),
                    textValue(profile, "mobile_phone"),
                    textValue(profile, "home_phone"),
                    textValue(profile, "site"),
                    integerValue(profile, "relation"),
                    textValue(profile, "education"),
                    jsonValue(profile, "career"),
                    jsonValue(profile, "counters"),
                    rawJson(profile)
            );
        }
        return null;
    }

    private JsonNode findArrayField(JsonNode node, String fieldName) {
        return findCollectionField(node, fieldName);
    }

    private JsonNode findObjectField(JsonNode node, String fieldName) {
        return findEntityField(node, fieldName);
    }

    private JsonNode findCollectionField(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        JsonNode direct = normalizeCollection(node.path(fieldName));
        if (direct != null) {
            return direct;
        }
        if (node.isObject()) {
            JsonNode labeledItems = labeledItems(node, fieldName);
            if (labeledItems != null) {
                return labeledItems;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode nested = findCollectionField(entry.getValue(), fieldName);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        if (!node.isArray()) {
            return null;
        }
        for (JsonNode item : node) {
            JsonNode nested = findCollectionField(item, fieldName);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private JsonNode findEntityField(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        JsonNode direct = normalizeEntity(node.path(fieldName));
        if (direct != null) {
            return direct;
        }
        if (node.isObject()) {
            JsonNode labeledItems = labeledItems(node, fieldName);
            JsonNode normalizedFromItems = firstEntity(labeledItems);
            if (normalizedFromItems != null) {
                return normalizedFromItems;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode nested = findEntityField(entry.getValue(), fieldName);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        if (!node.isArray()) {
            return null;
        }
        for (JsonNode item : node) {
            JsonNode nested = findEntityField(item, fieldName);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private JsonNode labeledItems(JsonNode node, String fieldName) {
        if (!node.isObject()) {
            return null;
        }
        String label = firstNonBlank(textValue(node, "type"), textValue(node, "kind"));
        if (!matchesFieldLabel(fieldName, label)) {
            return null;
        }
        return normalizeCollection(node.path("items"));
    }

    private boolean matchesFieldLabel(String fieldName, String label) {
        if (!StringUtils.hasText(fieldName) || !StringUtils.hasText(label)) {
            return false;
        }
        String normalizedField = fieldName.trim().toLowerCase();
        String normalizedLabel = label.trim().toLowerCase();
        if (normalizedField.equals(normalizedLabel)) {
            return true;
        }
        return singular(normalizedField).equals(singular(normalizedLabel));
    }

    private String singular(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("s") ? value.substring(0, value.length() - 1) : value;
    }

    private JsonNode normalizeCollection(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isArray()) {
            return node;
        }
        JsonNode items = node.path("items");
        if (!items.isMissingNode() && !items.isNull() && items != node) {
            JsonNode normalizedItems = normalizeCollection(items);
            if (normalizedItems != null) {
                return normalizedItems;
            }
        }
        if (!node.isObject()) {
            return null;
        }
        var values = node.elements();
        var arrayNode = objectMapper.createArrayNode();
        while (values.hasNext()) {
            JsonNode value = values.next();
            if (value == null || value.isNull() || value.isMissingNode()) {
                continue;
            }
            arrayNode.add(value);
        }
        return arrayNode.isEmpty() ? null : arrayNode;
    }

    private JsonNode normalizeEntity(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode items = node.path("items");
            if (!items.isMissingNode() && !items.isNull() && items.isObject()) {
                return firstEntity(items);
            }
            return node;
        }
        if (!node.isArray()) {
            return null;
        }
        for (JsonNode item : node) {
            JsonNode normalized = normalizeEntity(item);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private JsonNode firstEntity(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            if (node.hasNonNull("id") || node.hasNonNull("display_name") || node.hasNonNull("name")) {
                return node;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                JsonNode normalized = normalizeEntity(fields.next().getValue());
                if (normalized != null) {
                    return normalized;
                }
            }
            return null;
        }
        if (!node.isArray()) {
            return null;
        }
        for (JsonNode item : node) {
            JsonNode normalized = normalizeEntity(item);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
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

    private List<JsonNode> scriptPayloads(Document document) {
        List<JsonNode> payloads = new ArrayList<>();
        Set<String> seenPayloads = new LinkedHashSet<>();
        for (Element script : document.select("script")) {
            for (String candidate : scriptPayloadCandidates(script)) {
                if (!StringUtils.hasText(candidate) || !seenPayloads.add(candidate)) {
                    continue;
                }
                try {
                    JsonNode payload = objectMapper.readTree(candidate);
                    if (payload != null && !payload.isNull()) {
                        payloads.add(payload);
                    }
                } catch (JsonProcessingException ignored) {
                }
            }
        }
        return payloads;
    }

    private List<String> scriptPayloadCandidates(Element script) {
        List<String> candidates = new ArrayList<>();
        String scriptData = firstNonBlank(script.data(), script.html());
        if (!StringUtils.hasText(scriptData)) {
            return candidates;
        }
        if ("application/json".equalsIgnoreCase(script.attr("type"))) {
            candidates.add(scriptData.trim());
            return candidates;
        }
        String inlineObject = extractInlineJsonObject(scriptData);
        if (StringUtils.hasText(inlineObject)) {
            candidates.add(inlineObject);
            String normalized = normalizeJsLikeObject(inlineObject);
            if (StringUtils.hasText(normalized) && !normalized.equals(inlineObject)) {
                candidates.add(normalized);
            }
        }
        return candidates;
    }

    private String normalizeJsLikeObject(String payload) {
        if (!StringUtils.hasText(payload)) {
            return payload;
        }
        String normalized = payload;
        normalized = UNDEFINED_LITERAL.matcher(normalized).replaceAll("null");
        normalized = TRAILING_COMMA.matcher(normalized).replaceAll("");
        Matcher matcher = SINGLE_QUOTED_STRING.matcher(normalized);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String value = matcher.group(1)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("\"" + value + "\""));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String extractInlineJsonObject(String scriptData) {
        int assignmentIndex = scriptData.indexOf('=');
        int searchFrom = assignmentIndex >= 0 ? assignmentIndex + 1 : 0;
        int objectStart = scriptData.indexOf('{', searchFrom);
        if (objectStart < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = objectStart; i < scriptData.length(); i++) {
            char current = scriptData.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '{') {
                depth++;
                continue;
            }
            if (current != '}') {
                continue;
            }
            depth--;
            if (depth == 0) {
                return scriptData.substring(objectStart, i + 1).trim();
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
        String unixTime = element.attr("data-time");
        if (StringUtils.hasText(unixTime)) {
            return Instant.ofEpochSecond(Long.parseLong(unixTime));
        }
        return null;
    }

    private Integer parseSex(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.startsWith("жен")) {
            return 1;
        }
        if (normalized.startsWith("муж")) {
            return 2;
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

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.longValue() : parseLong(value.asText(null));
    }

    private Integer integerValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.intValue() : null;
    }

    private String textValue(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private String jsonValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return rawJson(value);
    }

    private String rawJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize VK fallback payload", ex);
        }
    }

    private String rawJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
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

    private Long firstNonNull(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Element firstNonNullElement(Element... elements) {
        for (Element element : elements) {
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return OffsetDateTime.parse(value).toInstant();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://vk.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
