package com.ca.centranalytics.integration.channel.max.wappi.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaxMessageDto(
        @JsonProperty("wh_type") String whType,
        @JsonProperty("profile_id") String profileId,
        String id,
        String body,
        String type,
        String from,
        String to,
        @JsonProperty("senderName") String senderName,
        @JsonProperty("chatId") String chatId,
        String timestamp,
        Long time,
        String caption,
        String mimetype,
        @JsonProperty("media_info") Object mediaInfo,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_link") String fileLink,
        @JsonProperty("is_forwarded") Boolean isForwarded,
        @JsonProperty("isReply") Boolean isReply,
        @JsonProperty("is_deleted") Boolean isDeleted,
        @JsonProperty("chat_type") String chatType,
        MaxLocationDto location,
        MaxContactDto contact,
        @JsonProperty("contact_name") String contactName,
        String title,
        String thumbnail,
        String picture
) {
}
