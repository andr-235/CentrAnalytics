package com.ca.centranalytics.integration.channel.telegram.authgateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramAuthGatewayConfirmRequest(
        String transactionId,
        String code,
        String password
) {
}
