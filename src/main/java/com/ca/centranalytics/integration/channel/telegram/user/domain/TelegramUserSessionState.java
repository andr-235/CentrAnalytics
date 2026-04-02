package com.ca.centranalytics.integration.channel.telegram.user.domain;

public enum TelegramUserSessionState {
    WAIT_PHONE,
    WAIT_CODE,
    WAIT_PASSWORD,
    READY,
    FAILED
}
