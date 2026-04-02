package com.ca.centranalytics.integration.channel.telegram.user.exception;

public class TelegramUserModeDisabledException extends RuntimeException {

    public TelegramUserModeDisabledException(String message) {
        super(message);
    }
}
