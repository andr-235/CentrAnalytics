package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.config.VkAutoCollectionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VkAutoCollectionScheduler {

    private final VkAutoCollectionProperties properties;
    private final VkAutoCollectionService vkAutoCollectionService;

    @Scheduled(fixedDelayString = "${integration.vk.auto-collection.fixed-delay-ms:900000}")
    public void runScheduledCollection() {
        if (!properties.enabled()) {
            return;
        }

        vkAutoCollectionService.collect();
    }
}
