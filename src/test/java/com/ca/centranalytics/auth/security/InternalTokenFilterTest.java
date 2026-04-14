package com.ca.centranalytics.auth.security;

import com.ca.centranalytics.integration.channel.telegram.gateway.config.TelegramGatewayIngestionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InternalTokenFilterTest {

    private static final String VALID_TOKEN = "test-internal-token-value";
    private static final String VALID_PATH = "/api/internal/integrations/telegram-user/events";
    private static final String OTHER_PATH = "/api/some/other/endpoint";

    private InternalTokenFilter filter;
    private TelegramGatewayIngestionProperties properties;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        properties = new TelegramGatewayIngestionProperties(true, VALID_TOKEN);
        filter = new InternalTokenFilter(properties, new ObjectMapper());
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void allowsRequestWithValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", VALID_PATH);
        request.addHeader("X-Internal-Token", VALID_TOKEN);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsRequestWithInvalidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", VALID_PATH);
        request.addHeader("X-Internal-Token", "wrong-token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or missing internal token");
    }

    @Test
    void rejectsRequestWithMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", VALID_PATH);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void skipsNonInternalPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", OTHER_PATH);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rejectsWhenFeatureDisabled() throws Exception {
        TelegramGatewayIngestionProperties disabledProperties =
                new TelegramGatewayIngestionProperties(false, VALID_TOKEN);
        filter = new InternalTokenFilter(disabledProperties, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", VALID_PATH);
        request.addHeader("X-Internal-Token", VALID_TOKEN);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(503);
    }
}
