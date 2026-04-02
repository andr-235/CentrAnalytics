package com.ca.centranalytics.integration.channel.telegram.user.api;

import jakarta.validation.constraints.NotBlank;

public record SubmitTelegramPasswordRequest(
        @NotBlank(message = "password is required")
        String password
) {
}
