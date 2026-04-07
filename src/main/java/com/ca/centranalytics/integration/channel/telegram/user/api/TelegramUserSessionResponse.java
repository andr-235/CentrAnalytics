package com.ca.centranalytics.integration.channel.telegram.user.api;

import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionState;

import java.time.Instant;

public record TelegramUserSessionResponse(
        String id,
        String phoneNumber,
        Long telegramUserId,
        TelegramUserSessionState state,
        boolean authorized,
        String errorMessage,
        Instant lastSyncAt
) {
}
