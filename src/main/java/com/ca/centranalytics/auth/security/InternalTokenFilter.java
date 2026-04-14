package com.ca.centranalytics.auth.security;

import com.ca.centranalytics.integration.channel.telegram.gateway.config.TelegramGatewayIngestionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Filter that validates internal service-to-service tokens for /api/internal/** endpoints.
 * Runs before the JWT authentication filter to provide defense-in-depth.
 */
public class InternalTokenFilter extends OncePerRequestFilter {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final TelegramGatewayIngestionProperties properties;
    private final ObjectMapper objectMapper;

    public InternalTokenFilter(TelegramGatewayIngestionProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Only apply to /api/internal/** paths
        if (!requestPath.startsWith("/api/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!properties.enabled()) {
            sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Internal API is disabled");
            return;
        }

        String internalToken = request.getHeader(INTERNAL_TOKEN_HEADER);

        if (!StringUtils.hasText(properties.internalToken())) {
            sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Internal token not configured");
            return;
        }

        if (!MessageDigest.isEqual(
                properties.internalToken().getBytes(StandardCharsets.UTF_8),
                (internalToken != null ? internalToken : "").getBytes(StandardCharsets.UTF_8))) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing internal token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = objectMapper.writeValueAsString(Map.of("error", message));
        response.getWriter().write(body);
    }
}
