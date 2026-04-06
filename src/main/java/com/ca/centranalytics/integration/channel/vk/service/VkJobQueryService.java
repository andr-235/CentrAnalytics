package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.api.VkCrawlJobStatusResponse;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VkJobQueryService {

    private final VkCrawlJobService vkCrawlJobService;

    public VkCrawlJobStatusResponse getJob(Long id) {
        VkCrawlJob job = vkCrawlJobService.get(id);
        return new VkCrawlJobStatusResponse(
                job.getId(),
                job.getJobType(),
                job.getStatus(),
                job.getItemCount(),
                job.getProcessedCount(),
                job.getErrorCount(),
                job.getWarningCount()
        );
    }
}
