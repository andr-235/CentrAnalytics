package com.ca.centranalytics.integration.channel.telegram.user.service;

import com.ca.centranalytics.integration.channel.telegram.user.api.StartTelegramUserSessionRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.SubmitTelegramAuthCodeRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.SubmitTelegramPasswordRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.TelegramUserSessionResponse;
import com.ca.centranalytics.integration.channel.telegram.authgateway.client.TelegramAuthGatewayClient;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewaySessionResponse;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewayStartResponse;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionState;
import com.ca.centranalytics.integration.channel.telegram.user.exception.TelegramUserModeDisabledException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramUserAuthService {

    private final TelegramAuthGatewayClient telegramAuthGatewayClient;

    public TelegramUserSessionResponse start(StartTelegramUserSessionRequest request) {
        TelegramAuthGatewayStartResponse response = telegramAuthGatewayClient.startSession(request.phoneNumber());
        return new TelegramUserSessionResponse(
                response.transactionId(),
                request.phoneNumber(),
                null,
                TelegramUserSessionState.WAIT_CODE,
                false,
                null,
                null
        );
    }

    public TelegramUserSessionResponse submitCode(String sessionId, SubmitTelegramAuthCodeRequest request) {
        try {
            TelegramAuthGatewaySessionResponse response = telegramAuthGatewayClient.confirmSession(sessionId, request.code(), null);
            return toReadyResponse(sessionId, response);
        } catch (IllegalArgumentException exception) {
            if ("PASSWORD_REQUIRED".equals(exception.getMessage())) {
                return new TelegramUserSessionResponse(
                        sessionId,
                        null,
                        null,
                        TelegramUserSessionState.WAIT_PASSWORD,
                        false,
                        null,
                        null
                );
            }
            throw exception;
        }
    }

    public TelegramUserSessionResponse submitPassword(String sessionId, SubmitTelegramPasswordRequest request) {
        TelegramAuthGatewaySessionResponse response = telegramAuthGatewayClient.confirmSession(sessionId, "", request.password());
        return toReadyResponse(sessionId, response);
    }

    public TelegramUserSessionResponse getStatus(String sessionId) {
        TelegramUserSessionResponse currentSession = getCurrentSession();
        if (currentSession != null && sessionId.equals(currentSession.id())) {
            return currentSession;
        }
        throw new IllegalArgumentException("Telegram user session " + sessionId + " not found");
    }

    public TelegramUserSessionResponse getCurrentSession() {
        TelegramAuthGatewaySessionResponse response = telegramAuthGatewayClient.getCurrentSession();
        if (response == null) {
            return null;
        }

        return new TelegramUserSessionResponse(
                "current",
                response.phoneNumber(),
                response.userId(),
                TelegramUserSessionState.READY,
                true,
                null,
                null
        );
    }

    public void ensureEnabled() {
        try {
            telegramAuthGatewayClient.getCurrentSession();
        } catch (IllegalArgumentException exception) {
            if ("Telegram auth gateway is disabled".equals(exception.getMessage())) {
                throw new TelegramUserModeDisabledException("Telegram auth gateway is disabled");
            }
        }
    }

    private TelegramUserSessionResponse toReadyResponse(String sessionId, TelegramAuthGatewaySessionResponse response) {
        return new TelegramUserSessionResponse(
                sessionId,
                response.phoneNumber(),
                response.userId(),
                TelegramUserSessionState.READY,
                true,
                null,
                null
        );
    }
}
