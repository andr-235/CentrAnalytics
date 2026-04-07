package com.ca.centranalytics.integration.channel.telegram.authgateway.client;

import com.ca.centranalytics.integration.channel.telegram.authgateway.config.TelegramAuthGatewayProperties;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewaySessionResponse;
import com.ca.centranalytics.integration.channel.telegram.authgateway.dto.TelegramAuthGatewayStartResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramAuthGatewayClientTest {

    private TelegramAuthGatewayClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TelegramAuthGatewayClient(
                builder,
                new TelegramAuthGatewayProperties(
                        true,
                        "http://telegram-auth-gateway:8091",
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(30)
                ),
                new ObjectMapper()
        );
    }

    @Test
    void startsSession() {
        server.expect(requestTo("http://telegram-auth-gateway:8091/session/start"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"transactionId":"tx-1","nextType":"app","codeLength":5,"timeoutSec":null}
                        """, APPLICATION_JSON));

        TelegramAuthGatewayStartResponse response = client.startSession("+79990000001");

        assertThat(response.transactionId()).isEqualTo("tx-1");
        assertThat(response.nextType()).isEqualTo("app");
        assertThat(response.codeLength()).isEqualTo(5);
    }

    @Test
    void returnsCurrentSession() {
        server.expect(requestTo("http://telegram-auth-gateway:8091/session/current"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"session":"saved","userId":123,"username":"centr","phoneNumber":"+79990000001"}
                        """, APPLICATION_JSON));

        TelegramAuthGatewaySessionResponse response = client.getCurrentSession();

        assertThat(response.userId()).isEqualTo(123L);
        assertThat(response.phoneNumber()).isEqualTo("+79990000001");
    }

    @Test
    void mapsGatewayErrorsToIllegalArgumentException() {
        server.expect(requestTo("http://telegram-auth-gateway:8091/session/start"))
                .andExpect(method(POST))
                .andRespond(withBadRequest()
                        .contentType(APPLICATION_JSON)
                        .body("""
                                {"error":"PASSWORD_REQUIRED"}
                                """));

        assertThatThrownBy(() -> client.startSession("+79990000001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PASSWORD_REQUIRED");
    }
}
