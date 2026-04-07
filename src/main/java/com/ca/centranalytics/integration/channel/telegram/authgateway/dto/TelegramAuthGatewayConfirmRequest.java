package com.ca.centranalytics.integration.channel.telegram.authgateway.dto;

public record TelegramAuthGatewayConfirmRequest(
        String transactionId,
        String code,
        String password
) {
}
