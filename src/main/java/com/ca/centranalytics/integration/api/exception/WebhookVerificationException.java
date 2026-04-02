package com.ca.centranalytics.integration.api.exception;

public class WebhookVerificationException extends RuntimeException {
    public WebhookVerificationException(String message) {
        super(message);
    }
}
