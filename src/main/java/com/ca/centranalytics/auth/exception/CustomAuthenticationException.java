package com.ca.centranalytics.auth.exception;

/**
 * Custom authentication exception to avoid naming conflict with Spring Security's AuthenticationException.
 */
public class CustomAuthenticationException extends RuntimeException {
    public CustomAuthenticationException(String message) {
        super(message);
    }
}
