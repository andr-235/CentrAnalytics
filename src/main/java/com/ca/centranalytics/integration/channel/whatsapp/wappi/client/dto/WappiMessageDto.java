package com.ca.centranalytics.integration.channel.whatsapp.wappi.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WappiMessageDto(
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
        @JsonProperty("from_where") String fromWhere,
        String mimetype,
        @JsonProperty("contact_name") String contactName,
        @JsonProperty("is_forwarded") Boolean isForwarded,
        @JsonProperty("isReply") Boolean isReply,
        @JsonProperty("is_edited") Boolean isEdited,
        @JsonProperty("stanza_id") String stanzaId,
        @JsonProperty("is_me") Boolean isMe,
        @JsonProperty("chat_type") String chatType,
        @JsonProperty("file_name") String fileName,
        String title,
        @JsonProperty("reply_message") WappiReplyMessageDto replyMessage,
        String thumbnail,
        String picture,
        @JsonProperty("wappi_bot_id") String wappiBotId,
        @JsonProperty("is_deleted") Boolean isDeleted,
        @JsonProperty("is_bot") Boolean isBot
) {
}
