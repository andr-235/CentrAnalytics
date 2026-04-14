package com.ca.centranalytics.integration.channel.vk.client;

import com.ca.centranalytics.integration.channel.vk.client.dto.VkCommentResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkGroupSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkUserSearchResult;
import com.ca.centranalytics.integration.channel.vk.client.dto.VkWallPostResult;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.users.UserFull;
import com.vk.api.sdk.objects.wall.WallComment;
import com.vk.api.sdk.objects.wall.WallItem;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Маппер для преобразования объектов VK SDK в DTO проекта.
 * <p>
 * Отвечает за преобразование ответов от VK Official API во внутренние
 * структуры данных приложения.
 */
public class VkOfficialResponseMapper {

    private final VkApiClient vkApiClient;

    public VkOfficialResponseMapper(VkApiClient vkApiClient) {
        this.vkApiClient = vkApiClient;
    }

    /**
     * Преобразует группу из VK SDK в {@link VkGroupSearchResult}.
     *
     * @param group группа из VK SDK
     * @return DTO с данными группы
     */
    public VkGroupSearchResult toGroupResult(GroupFull group) {
        return new VkGroupSearchResult(
                group.getId(),
                group.getName(),
                group.getScreenName(),
                group.getDescription(),
                group.getCity() != null ? group.getCity().getTitle() : null,
                vkApiClient.getGson().toJson(group)
        );
    }

    /**
     * Преобразует пользователя из VK SDK в {@link VkUserSearchResult}.
     * <p>
     * Извлекает все доступные поля профиля включая:
     * <ul>
     *   <li>Основную информацию (имя, фамилия, screen name)</li>
     *   <li>Контактные данные (телефон, сайт)</li>
     *   <li>Образование и карьеру</li>
     *   <li>Город, домашний город, дату рождения</li>
     * </ul>
     *
     * @param user пользователь из VK SDK
     * @return DTO с данными пользователя
     */
    public VkUserSearchResult toUserResult(UserFull user) {
        long userId = user.getId();
        String screenName = firstNonBlank(user.getScreenName(), user.getDomain());
        String profileUrl = "https://vk.com/" + firstNonBlank(screenName, "id" + userId);
        String displayName = firstNonBlank(user.getNickname(), joinNonBlank(user.getFirstName(), user.getLastName()));
        String education = firstNonBlank(user.getUniversityName(), user.getFacultyName());

        return new VkUserSearchResult(
                userId,
                displayName,
                user.getFirstName(),
                user.getLastName(),
                screenName,
                profileUrl,
                user.getCity() != null ? user.getCity().getTitle() : null,
                user.getHomeTown(),
                user.getBdate(),
                user.getSex() != null ? user.getSex().getValue() : null,
                user.getStatus(),
                user.getLastSeen() != null && user.getLastSeen().getTime() != null ? Instant.ofEpochSecond(user.getLastSeen().getTime()) : null,
                user.getPhoto200() != null ? user.getPhoto200().toString() : null,
                user.getMobilePhone(),
                user.getHomePhone(),
                user.getSite(),
                user.getRelation() != null ? user.getRelation().getValue() : null,
                education,
                user.getCareer() != null ? vkApiClient.getGson().toJson(user.getCareer()) : null,
                user.getCounters() != null ? vkApiClient.getGson().toJson(user.getCounters()) : null,
                vkApiClient.getGson().toJson(user)
        );
    }

    /**
     * Преобразует запись со стены из VK SDK в {@link VkWallPostResult}.
     *
     * @param item запись со стены из VK SDK
     * @return DTO с данными поста
     */
    public VkWallPostResult toWallPostResult(WallItem item) {
        return new VkWallPostResult(
                item.getOwnerId(),
                Long.valueOf(item.getId()),
                positiveOrNull(item.getFromId()),
                item.getText(),
                parseUnixTimestamp(item.getDate()),
                vkApiClient.getGson().toJson(item)
        );
    }

    /**
     * Преобразует комментарий из VK SDK в {@link VkCommentResult}.
     *
     * @param comment комментарий из VK SDK
     * @return DTO с данными комментария
     */
    public VkCommentResult toCommentResult(WallComment comment) {
        return new VkCommentResult(
                comment.getOwnerId(),
                comment.getPostId().longValue(),
                comment.getId().longValue(),
                positiveOrNull(comment.getFromId()),
                comment.getText(),
                parseUnixTimestamp(comment.getDate()),
                vkApiClient.getGson().toJson(comment)
        );
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
}
