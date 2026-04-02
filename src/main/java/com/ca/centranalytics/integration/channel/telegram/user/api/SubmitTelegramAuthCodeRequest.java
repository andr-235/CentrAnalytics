package com.ca.centranalytics.integration.channel.telegram.user.api;

import jakarta.validation.constraints.NotBlank;

public record SubmitTelegramAuthCodeRequest(
        @NotBlank(message = "code is required")
        String code
) {
}
