package com.ca.centranalytics.integration.channel.telegram.authgateway.dto;

public record TelegramAuthGatewayStartResponse(
        String transactionId,
        String nextType,
        Integer codeLength,
        Integer timeoutSec
) {
}
