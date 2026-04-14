package com.ca.centranalytics.auth.security;

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
 * Validates webhook request signatures to prevent unauthorized data injection.
 * Supports signature verification via X-Webhook-Signature header with HMAC-SHA256.
 */
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    public WebhookSignatureFilter(String webhookSecret, ObjectMapper objectMapper) {
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String signature = request.getHeader(SIGNATURE_HEADER);

        if (!StringUtils.hasText(webhookSecret)) {
            // If no secret is configured, allow the request (development mode)
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(signature)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing webhook signature");
            return;
        }

        // Read request body for signature verification
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (!isSignatureValid(body, signature, webhookSecret)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid webhook signature");
            return;
        }

        // Re-wrap the request body so downstream controllers can read it
        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request, body);
        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean isSignatureValid(String body, String providedSignature, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] expectedMac = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] providedMac = hexToBytes(providedSignature);
            return MessageDigest.isEqual(expectedMac, providedMac);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = objectMapper.writeValueAsString(Map.of("error", message));
        response.getWriter().write(body);
    }
}
