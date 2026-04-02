package com.ca.centranalytics.integration.channel.telegram.user.exception;

public class TelegramUserSessionConflictException extends RuntimeException {

    public TelegramUserSessionConflictException(String message) {
        super(message);
    }
}
