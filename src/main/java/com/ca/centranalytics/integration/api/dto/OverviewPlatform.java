package com.ca.centranalytics.integration.api.dto;

import com.ca.centranalytics.integration.domain.entity.Platform;

public enum OverviewPlatform {
    TELEGRAM("TELEGRAM", "Telegram", Platform.TELEGRAM),
    VK("VK", "VK", Platform.VK),
    WHATSAPP("WHATSAPP", "WhatsApp", Platform.WAPPI),
    MAX("MAX", "Max", Platform.MAX);

    private final String apiValue;
    private final String label;
    private final Platform internalPlatform;

    OverviewPlatform(String apiValue, String label, Platform internalPlatform) {
        this.apiValue = apiValue;
        this.label = label;
        this.internalPlatform = internalPlatform;
    }

    public String apiValue() {
        return apiValue;
    }

    public String label() {
        return label;
    }

    public Platform internalPlatform() {
        return internalPlatform;
    }
}
