package com.ca.centranalytics.integration.channel.vk.client;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkRegionalCity;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.ca.centranalytics.integration.channel.vk.config.VkProperties;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.database.City;
import com.vk.api.sdk.objects.database.Region;
import com.vk.api.sdk.objects.database.responses.GetCitiesResponse;
import com.vk.api.sdk.objects.database.responses.GetRegionsResponse;
import com.vk.api.sdk.objects.groups.SearchType;
import com.vk.api.sdk.objects.groups.responses.SearchResponse;
import com.vk.api.sdk.objects.users.Fields;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import com.vk.api.sdk.objects.wall.responses.GetCommentsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class HttpVkOfficialClient implements VkOfficialClient {

    private static final long SDK_ACTOR_ID = 1L;
    private static final String EAO_REGION = "Еврейская автономная область";
    private static final int VK_DATABASE_PAGE_SIZE = 1000;
    private static final int TOO_MANY_REQUESTS_ERROR_CODE = 6;
    private static final int FLOOD_CONTROL_ERROR_CODE = 9;
    private static final int RATE_LIMIT_REACHED_ERROR_CODE = 29;
    private static final Duration DEFAULT_RETRY_BASE_DELAY = Duration.ofSeconds(1);
    private static final Duration DEFAULT_RETRY_MAX_DELAY = Duration.ofSeconds(30);

    private final VkProperties vkProperties;
    private final VkApiClient vkApiClient;
    private final VkOfficialResponseMapper responseMapper;
    private final Object requestThrottleMonitor = new Object();

    private long nextAllowedRequestAtMs;

    @Autowired
    public HttpVkOfficialClient(VkProperties vkProperties) {
        this(vkProperties, new VkApiClient(new ConfigurableTransportClient(vkProperties)));
    }

    public static HttpVkOfficialClient withVkApiClient(VkProperties vkProperties, VkApiClient vkApiClient) {
        return new HttpVkOfficialClient(vkProperties, vkApiClient, new VkOfficialResponseMapper(vkApiClient));
    }

    HttpVkOfficialClient(VkProperties vkProperties, VkApiClient vkApiClient) {
        this(vkProperties, vkApiClient, new VkOfficialResponseMapper(vkApiClient));
    }

    HttpVkOfficialClient(VkProperties vkProperties, VkApiClient vkApiClient, VkOfficialResponseMapper responseMapper) {
        this.vkProperties = vkProperties;
        this.vkApiClient = vkApiClient;
        this.responseMapper = responseMapper;
    }

    @Override
    public List<String> resolveRegionalSearchTerms(String region) {
        return resolveRegionalCities(region).stream()
                .map(VkRegionalCity::title)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Override
    public List<VkRegionalCity> resolveRegionalCities(String region) {
        if (!StringUtils.hasText(region)) {
            return List.of();
        }
        if (!isEaoRegion(region)) {
            return List.of(new VkRegionalCity(null, region));
        }

        Integer regionId = resolveRegionId(region);
        if (regionId == null) {
            return List.of(new VkRegionalCity(null, region));
        }

        Set<VkRegionalCity> regionalCities = new LinkedHashSet<>();
        int offset = 0;
        while (true) {
            int currentOffset = offset;
            GetCitiesResponse response = execute(() -> vkApiClient.database()
                    .getCities(userActorForUserCalls())
                    .regionId(regionId)
                    .needAll(true)
                    .offset(currentOffset)
                    .count(VK_DATABASE_PAGE_SIZE)
                    .execute());

            List<City> pageCities = response.getItems();
            if (pageCities == null || pageCities.isEmpty()) {
                break;
            }

            pageCities.stream()
                    .filter(city -> city.getId() != null)
                    .map(city -> new VkRegionalCity(city.getId(), city.getTitle()))
                    .filter(city -> StringUtils.hasText(city.title()))
                    .forEach(regionalCities::add);

            offset += pageCities.size();
            if (pageCities.size() < VK_DATABASE_PAGE_SIZE) {
                break;
            }
        }

        return regionalCities.isEmpty() ? List.of(new VkRegionalCity(null, region)) : List.copyOf(regionalCities);
    }

    @Override
    public List<VkGroupSearchResult> searchGroups(String region, int limit) {
        SearchResponse response = execute(() -> vkApiClient.groups()
                .search(userActorForOfficialCalls(), region)
                .type(SearchType.GROUP)
                .count(limit)
                .execute());

        return response.getItems().stream()
                .map(responseMapper::toGroupResult)
                .toList();
    }

    @Override
    public List<VkUserSearchResult> searchUsers(String region, int limit) {
        return searchUsers(new VkRegionalCity(null, region), limit);
    }

    @Override
    public List<VkUserSearchResult> searchUsers(VkRegionalCity city, int limit) {
        com.vk.api.sdk.queries.users.UsersSearchQuery query = vkApiClient.users()
                .search(userActorForUserCalls())
                .q(city.title())
                .count(limit)
                .fields(userFields());
        if (city.id() != null) {
            query.cityId(city.id());
        }
        com.vk.api.sdk.objects.users.responses.SearchResponse response = execute(query::execute);

        return response.getItems().stream()
                .map(responseMapper::toUserResult)
                .toList();
    }

    @Override
    public List<VkWallPostResult> getGroupPosts(String domain, int limit) {
        com.vk.api.sdk.objects.wall.responses.GetResponse response = execute(() -> vkApiClient.wall()
                .get(userActorForOfficialCalls())
                .domain(domain)
                .count(limit)
                .execute());

        return response.getItems().stream()
                .map(responseMapper::toWallPostResult)
                .toList();
    }

    @Override
    public List<VkCommentResult> getPostComments(Long ownerId, Long postId, int limit) {
        GetCommentsResponse response = execute(() -> vkApiClient.wall()
                .getComments(userActorForOfficialCalls())
                .ownerId(ownerId)
                .postId(postId.intValue())
                .count(limit)
                .threadItemsCount(0)
                .execute());

        return response.getItems().stream()
                .map(responseMapper::toCommentResult)
                .toList();
    }

    @Override
    public List<VkUserSearchResult> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<GetResponse> response = execute(() -> vkApiClient.users()
                .get(userActorForUserCalls())
                .userIds(userIds.stream().map(String::valueOf).toList())
                .fields(userFields())
                .execute());

        return response.stream()
                .map(responseMapper::toUserResult)
                .toList();
    }

    private UserActor userActorForOfficialCalls() {
        return new UserActor(SDK_ACTOR_ID, officialAccessToken());
    }

    private UserActor userActorForUserCalls() {
        return new UserActor(SDK_ACTOR_ID, officialAccessToken());
    }

    private Integer resolveRegionId(String region) {
        GetRegionsResponse response = execute(() -> vkApiClient.database()
                .getRegions(userActorForUserCalls())
                .q(region)
                .count(VK_DATABASE_PAGE_SIZE)
                .execute());

        if (response.getItems() == null) {
            return null;
        }

        return response.getItems().stream()
                .filter(candidate -> isSameRegionTitle(region, candidate))
                .map(Region::getId)
                .findFirst()
                .orElse(null);
    }

    private String officialAccessToken() {
        if (!StringUtils.hasText(vkProperties.accessToken())) {
            throw new IllegalStateException("integration.vk.access-token is required for VK API calls");
        }
        return vkProperties.accessToken();
    }

    private Fields[] userFields() {
        return new Fields[]{
                Fields.CITY,
                Fields.HOME_TOWN,
                Fields.SCREEN_NAME,
                Fields.DOMAIN,
                Fields.BDATE,
                Fields.SEX,
                Fields.STATUS,
                Fields.LAST_SEEN,
                Fields.PHOTO_200,
                Fields.CONTACTS,
                Fields.CONNECTIONS,
                Fields.CAREER,
                Fields.EDUCATION,
                Fields.RELATION,
                Fields.COUNTERS
        };
    }

    private <T> T execute(VkSdkCall<T> call) {
        int attempts = configuredRetryAttempts();
        for (int attempt = 1; attempt <= attempts; attempt++) {
            throttleBeforeRequest();
            try {
                return call.execute();
            } catch (ApiException ex) {
                if (isRateLimitError(ex) && attempt < attempts) {
                    sleep(calculateRetryDelay(attempt), "VK retry interrupted");
                    continue;
                }
                throw new IllegalStateException("VK SDK call failed", ex);
            } catch (ClientException ex) {
                throw new IllegalStateException("VK SDK call failed", ex);
            }
        }
        throw new IllegalStateException("VK SDK call failed");
    }

    private void throttleBeforeRequest() {
        long minimumIntervalMs = configuredMinimumRequestIntervalMs();
        if (minimumIntervalMs <= 0) {
            return;
        }

        synchronized (requestThrottleMonitor) {
            long now = System.currentTimeMillis();
            long waitMs = nextAllowedRequestAtMs - now;
            if (waitMs > 0) {
                sleep(waitMs, "VK request throttling interrupted");
            }
            nextAllowedRequestAtMs = System.currentTimeMillis() + minimumIntervalMs;
        }
    }

    private boolean isRateLimitError(ApiException ex) {
        Integer code = ex.getCode();
        return code != null
                && (code == TOO_MANY_REQUESTS_ERROR_CODE
                || code == FLOOD_CONTROL_ERROR_CODE
                || code == RATE_LIMIT_REACHED_ERROR_CODE);
    }

    private long calculateRetryDelay(int attempt) {
        long baseDelayMs = configuredRetryBaseDelayMs();
        long maxDelayMs = configuredRetryMaxDelayMs();
        long multiplier = 1L << Math.min(attempt - 1, 30);
        long scaledDelay;
        try {
            scaledDelay = Math.multiplyExact(baseDelayMs, multiplier);
        } catch (ArithmeticException ex) {
            scaledDelay = Long.MAX_VALUE;
        }
        return Math.min(scaledDelay, maxDelayMs);
    }

    private int configuredRetryAttempts() {
        return Math.max(1, vkProperties.rateLimitRetryAttempts());
    }

    private long configuredMinimumRequestIntervalMs() {
        return positiveDurationMs(vkProperties.minimumRequestInterval(), Duration.ZERO);
    }

    private long configuredRetryBaseDelayMs() {
        return positiveDurationMs(vkProperties.rateLimitRetryBaseDelay(), DEFAULT_RETRY_BASE_DELAY);
    }

    private long configuredRetryMaxDelayMs() {
        long maxDelayMs = positiveDurationMs(vkProperties.rateLimitRetryMaxDelay(), DEFAULT_RETRY_MAX_DELAY);
        return Math.max(maxDelayMs, configuredRetryBaseDelayMs());
    }

    private long positiveDurationMs(Duration value, Duration fallback) {
        Duration effective = value == null || value.isNegative() ? fallback : value;
        return Math.max(0L, effective.toMillis());
    }

    private void sleep(long durationMs, String interruptionMessage) {
        if (durationMs <= 0) {
            return;
        }
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interruptionMessage, ex);
        }
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

    private Instant parseUnixTimestamp(Integer value) {
        return value != null ? Instant.ofEpochSecond(value) : null;
    }

    private Long positiveOrNull(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private boolean isEaoRegion(String region) {
        return normalize(region).equals(normalize(EAO_REGION));
    }

    private boolean isSameRegionTitle(String requestedRegion, Region candidate) {
        return candidate != null
                && candidate.getId() != null
                && StringUtils.hasText(candidate.getTitle())
                && normalize(candidate.getTitle()).equals(normalize(requestedRegion));
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
    }

    @FunctionalInterface
    private interface VkSdkCall<T> {
        T execute() throws ApiException, ClientException;
    }

    static final class ConfigurableTransportClient implements TransportClient {

        private final String apiBaseUrl;
        private final HttpTransportClient delegate;

        ConfigurableTransportClient(VkProperties vkProperties) {
            this.apiBaseUrl = normalizeApiBaseUrl(vkProperties.apiBaseUrl());
            this.delegate = HttpTransportClient.getInstance();
        }

        @Override
        public ClientResponse get(String url) throws IOException {
            return delegate.get(rewriteUrl(url));
        }

        @Override
        public ClientResponse get(String url, String body) throws IOException {
            return delegate.get(rewriteUrl(url), body);
        }

        @Override
        public ClientResponse get(String url, org.apache.http.Header[] headers) throws IOException {
            return delegate.get(rewriteUrl(url), headers);
        }

        @Override
        public ClientResponse post(String url, String body) throws IOException {
            return delegate.post(rewriteUrl(url), body);
        }

        @Override
        public ClientResponse post(String url, Map<String, java.io.File> files) throws IOException {
            return delegate.post(rewriteUrl(url), files);
        }

        @Override
        public ClientResponse post(String url, String body, String contentType) throws IOException {
            return delegate.post(rewriteUrl(url), body, contentType);
        }

        @Override
        public ClientResponse post(String url) throws IOException {
            return delegate.post(rewriteUrl(url));
        }

        @Override
        public ClientResponse post(String url, String body, org.apache.http.Header[] headers) throws IOException {
            return delegate.post(rewriteUrl(url), body, headers);
        }

        @Override
        public ClientResponse delete(String url) throws IOException {
            return delegate.delete(rewriteUrl(url));
        }

        @Override
        public ClientResponse delete(String url, String body) throws IOException {
            return delegate.delete(rewriteUrl(url), body);
        }

        @Override
        public ClientResponse delete(String url, String body, String contentType) throws IOException {
            return delegate.delete(rewriteUrl(url), body, contentType);
        }

        @Override
        public ClientResponse delete(String url, String body, org.apache.http.Header[] headers) throws IOException {
            return delegate.delete(rewriteUrl(url), body, headers);
        }

        private String rewriteUrl(String url) {
            if (!StringUtils.hasText(apiBaseUrl)) {
                return url;
            }
            return url.replace("https://api.vk.com/method", apiBaseUrl);
        }

        private static String normalizeApiBaseUrl(String apiBaseUrl) {
            if (!StringUtils.hasText(apiBaseUrl)) {
                return null;
            }
            return apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        }
    }
}
