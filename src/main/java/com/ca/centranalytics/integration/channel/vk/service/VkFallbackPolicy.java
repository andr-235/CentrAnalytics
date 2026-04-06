package com.ca.centranalytics.integration.channel.vk.service;

import org.springframework.stereotype.Service;

@Service
public class VkFallbackPolicy {

    public boolean isFallbackAllowed(String collectionMode) {
        return "HYBRID".equalsIgnoreCase(collectionMode);
    }

    public boolean shouldFallbackForSearch(String collectionMode, boolean officialEmpty, boolean dataIncomplete) {
        return isFallbackAllowed(collectionMode) && (officialEmpty || dataIncomplete);
    }
}
