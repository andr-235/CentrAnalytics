package com.ca.centranalytics.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WebhookSignatureFilterTest {

    private static final String WEBHOOK_SECRET = "webhook-test-secret-key";
    private static final String VALID_PATH = "/api/integrations/webhooks/wappi";

    private WebhookSignatureFilter filter;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new WebhookSignatureFilter(WEBHOOK_SECRET, objectMapper);
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void allowsRequestWithValidSignature() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", "test"));
        String signature = generateHmacSignature(body, WEBHOOK_SECRET);

        MockHttpServletRequest request = createRequestWithBody(body);
        request.addHeader("X-Webhook-Signature", signature);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void rejectsRequestWithInvalidSignature() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", "test"));

        MockHttpServletRequest request = createRequestWithBody(body);
        request.addHeader("X-Webhook-Signature", "invalid-signature");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid webhook signature");
    }

    @Test
    void rejectsRequestWithMissingSignature() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("message", "test"));

        MockHttpServletRequest request = createRequestWithBody(body);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Missing webhook signature");
    }

    @Test
    void allowsRequestWhenNoSecretConfigured() throws Exception {
        WebhookSignatureFilter noSecretFilter = new WebhookSignatureFilter("", objectMapper);
        String body = objectMapper.writeValueAsString(Map.of("message", "test"));

        MockHttpServletRequest request = createRequestWithBody(body);

        noSecretFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    private String generateHmacSignature(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmacBytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hmacBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private MockHttpServletRequest createRequestWithBody(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", VALID_PATH);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
