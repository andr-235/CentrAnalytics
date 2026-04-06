package com.ca.centranalytics.integration.channel.vk.api;

import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;

public record VkCrawlJobStatusResponse(
        Long id,
        VkCrawlJobType jobType,
        VkCrawlJobStatus status,
        int itemCount,
        int processedCount,
        int errorCount,
        int warningCount
) {
}
