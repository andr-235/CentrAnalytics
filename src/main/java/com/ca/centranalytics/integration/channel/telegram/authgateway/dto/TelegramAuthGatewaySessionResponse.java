package com.ca.centranalytics.integration.channel.telegram.authgateway.dto;

public record TelegramAuthGatewaySessionResponse(
        String session,
        Long userId,
        String username,
        String phoneNumber
) {
}
