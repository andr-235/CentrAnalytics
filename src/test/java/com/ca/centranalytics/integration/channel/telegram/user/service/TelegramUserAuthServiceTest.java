package com.ca.centranalytics.integration.channel.telegram.user.service;

import com.ca.centranalytics.integration.channel.telegram.authgateway.client.TelegramAuthGatewayClient;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewaySessionResponse;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewayStartResponse;
import com.ca.centranalytics.integration.channel.telegram.user.api.StartTelegramUserSessionRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.SubmitTelegramAuthCodeRequest;
import com.ca.centranalytics.integration.channel.telegram.user.api.SubmitTelegramPasswordRequest;
import com.ca.centranalytics.integration.channel.telegram.user.domain.TelegramUserSessionState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class TelegramUserAuthServiceTest {

    @Test
    void startsSessionInWaitCodeState() {
        FakeTelegramAuthGatewayClient gatewayClient = new FakeTelegramAuthGatewayClient();
        gatewayClient.startResponse = new TelegramAuthGatewayStartResponse("tx-1", "app", 5, null);
        TelegramUserAuthService telegramUserAuthService = new TelegramUserAuthService(gatewayClient);

        var response = telegramUserAuthService.start(new StartTelegramUserSessionRequest("+79990000001"));

        assertThat(response.id()).isEqualTo("tx-1");
        assertThat(response.state()).isEqualTo(TelegramUserSessionState.WAIT_CODE);
        assertThat(response.authorized()).isFalse();
    }

    @Test
    void turnsPasswordRequiredIntoWaitPasswordState() {
        FakeTelegramAuthGatewayClient gatewayClient = new FakeTelegramAuthGatewayClient();
        gatewayClient.confirmException = new IllegalArgumentException("PASSWORD_REQUIRED");
        TelegramUserAuthService telegramUserAuthService = new TelegramUserAuthService(gatewayClient);

        var response = telegramUserAuthService.submitCode("tx-1", new SubmitTelegramAuthCodeRequest("12345"));

        assertThat(response.id()).isEqualTo("tx-1");
        assertThat(response.state()).isEqualTo(TelegramUserSessionState.WAIT_PASSWORD);
        assertThat(response.authorized()).isFalse();
    }

    @Test
    void marksSessionReadyAfterPasswordSubmit() {
        FakeTelegramAuthGatewayClient gatewayClient = new FakeTelegramAuthGatewayClient();
        gatewayClient.confirmResponse = new TelegramAuthGatewaySessionResponse(
                "session",
                123L,
                "centr",
                "+79990000001"
        );
        TelegramUserAuthService telegramUserAuthService = new TelegramUserAuthService(gatewayClient);

        var response = telegramUserAuthService.submitPassword("tx-1", new SubmitTelegramPasswordRequest("secret"));

        assertThat(response.state()).isEqualTo(TelegramUserSessionState.READY);
        assertThat(response.telegramUserId()).isEqualTo(123L);
        assertThat(response.phoneNumber()).isEqualTo("+79990000001");
    }

    @Test
    void throwsWhenRequestedStatusDoesNotMatchCurrentSession() {
        TelegramUserAuthService telegramUserAuthService = new TelegramUserAuthService(new FakeTelegramAuthGatewayClient());

        assertThatThrownBy(() -> telegramUserAuthService.getStatus("tx-missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telegram user session tx-missing not found");
    }

    private static final class FakeTelegramAuthGatewayClient extends TelegramAuthGatewayClient {

        private TelegramAuthGatewayStartResponse startResponse;
        private TelegramAuthGatewaySessionResponse confirmResponse;
        private TelegramAuthGatewaySessionResponse currentResponse;
        private IllegalArgumentException confirmException;

        private FakeTelegramAuthGatewayClient() {
            super(org.springframework.web.client.RestClient.builder(),
                    new com.ca.centranalytics.integration.channel.telegram.authgateway.config.TelegramAuthGatewayProperties(
                            true,
                            "http://localhost",
                            java.time.Duration.ofSeconds(1),
                            java.time.Duration.ofSeconds(1)
                    ),
                    new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Override
        public TelegramAuthGatewayStartResponse startSession(String phoneNumber) {
            return startResponse;
        }

        @Override
        public TelegramAuthGatewaySessionResponse confirmSession(String transactionId, String code, String password) {
            if (confirmException != null) {
                throw confirmException;
            }
            return confirmResponse;
        }

        @Override
        public TelegramAuthGatewaySessionResponse getCurrentSession() {
            return currentResponse;
        }
    }
}
