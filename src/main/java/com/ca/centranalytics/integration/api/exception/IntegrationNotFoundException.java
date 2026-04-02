package com.ca.centranalytics.integration.api.exception;

public class IntegrationNotFoundException extends RuntimeException {
    public IntegrationNotFoundException(String message) {
        super(message);
    }
}
