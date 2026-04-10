package com.ca.centranalytics.integration.api.dto;

import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public enum OverviewWindow {
    HOURS_24("24h", Duration.ofHours(24)),
    DAYS_7("7d", Duration.ofDays(7)),
    DAYS_30("30d", Duration.ofDays(30));

    private final String apiValue;
    private final Duration duration;

    OverviewWindow(String apiValue, Duration duration) {
        this.apiValue = apiValue;
        this.duration = duration;
    }

    public String apiValue() {
        return apiValue;
    }

    public Duration duration() {
        return duration;
    }

    public static OverviewWindow fromApiValue(String value) {
        for (OverviewWindow candidate : values()) {
            if (candidate.apiValue.equalsIgnoreCase(value)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(BAD_REQUEST, "Unsupported overview window: " + value);
    }
}
