package com.ca.centranalytics.integration.channel.telegram.user.api;

import jakarta.validation.constraints.NotBlank;

public record StartTelegramUserSessionRequest(
        @NotBlank(message = "phoneNumber is required")
        String phoneNumber
) {
}
