package com.ca.centranalytics.integration.channel.whatsapp.wappi.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WappiReplyMessageDto(
        String id,
        String body,
        String type,
        @JsonProperty("chatId") String chatId,
        String timestamp,
        Long time,
        String caption,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("contact_name") String contactName
) {
}
