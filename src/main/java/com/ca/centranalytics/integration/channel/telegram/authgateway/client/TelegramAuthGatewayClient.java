package com.ca.centranalytics.integration.channel.telegram.authgateway.client;

import com.ca.centranalytics.integration.channel.telegram.authgateway.config.TelegramAuthGatewayProperties;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewayConfirmRequest;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewayErrorResponse;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewaySessionResponse;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewayStartRequest;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewayStartResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

@Component
@EnableConfigurationProperties(TelegramAuthGatewayProperties.class)
public class TelegramAuthGatewayClient {

    private final RestClient restClient;
    private final TelegramAuthGatewayProperties properties;
    private final ObjectMapper objectMapper;

    public TelegramAuthGatewayClient(
            RestClient.Builder restClientBuilder,
            TelegramAuthGatewayProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    public TelegramAuthGatewayStartResponse startSession(String phoneNumber) {
        ensureEnabled();
        try {
            return restClient.post()
                    .uri("/session/start")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TelegramAuthGatewayStartRequest(phoneNumber))
                    .retrieve()
                    .body(TelegramAuthGatewayStartResponse.class);
        } catch (RestClientResponseException exception) {
            throw toIllegalArgument(exception);
        }
    }

    public TelegramAuthGatewaySessionResponse confirmSession(String transactionId, String code, String password) {
        ensureEnabled();
        try {
            return restClient.post()
                    .uri("/session/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TelegramAuthGatewayConfirmRequest(transactionId, code, password))
                    .retrieve()
                    .body(TelegramAuthGatewaySessionResponse.class);
        } catch (RestClientResponseException exception) {
            throw toIllegalArgument(exception);
        }
    }

    public TelegramAuthGatewaySessionResponse getCurrentSession() {
        ensureEnabled();
        try {
            return restClient.get()
                    .uri("/session/current")
                    .retrieve()
                    .body(TelegramAuthGatewaySessionResponse.class);
        } catch (RestClientResponseException exception) {
            throw toIllegalArgument(exception);
        }
    }

    public void resetCurrentSession() {
        ensureEnabled();
        try {
            restClient.delete()
                    .uri("/session/current")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw toIllegalArgument(exception);
        }
    }

    private void ensureEnabled() {
        if (!properties.enabled()) {
            throw new IllegalArgumentException("Telegram auth gateway is disabled");
        }
    }

    private IllegalArgumentException toIllegalArgument(RestClientResponseException exception) {
        String message = exception.getStatusText();
        try {
            TelegramAuthGatewayErrorResponse response = objectMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    TelegramAuthGatewayErrorResponse.class
            );
            if (response != null && StringUtils.hasText(response.error())) {
                message = response.error();
            }
        } catch (IOException ignored) {
            if (StringUtils.hasText(exception.getResponseBodyAsString())) {
                message = exception.getResponseBodyAsString();
            }
        }
        return new IllegalArgumentException(message, exception);
    }
}
