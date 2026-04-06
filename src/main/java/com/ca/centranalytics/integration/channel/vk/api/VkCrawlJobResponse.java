package com.ca.centranalytics.integration.channel.vk.api;

import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;

public record VkCrawlJobResponse(
        Long id,
        VkCrawlJobType jobType,
        VkCrawlJobStatus status
) {
}
