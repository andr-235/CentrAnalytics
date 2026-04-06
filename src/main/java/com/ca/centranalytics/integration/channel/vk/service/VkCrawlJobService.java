package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.api.exception.IntegrationNotFoundException;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobType;
import com.ca.centranalytics.integration.channel.vk.repository.VkCrawlJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VkCrawlJobService {

    private final VkCrawlJobRepository vkCrawlJobRepository;
    private final ObjectMapper objectMapper;

    public VkCrawlJob create(VkCrawlJobType jobType, Object request) {
        return vkCrawlJobRepository.save(VkCrawlJob.builder()
                .jobType(jobType)
                .status(VkCrawlJobStatus.CREATED)
                .requestJson(writeValue(request))
                .build());
    }

    public VkCrawlJob get(Long id) {
        return vkCrawlJobRepository.findById(id)
                .orElseThrow(() -> new IntegrationNotFoundException("VK crawl job not found: " + id));
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize VK crawl request", ex);
        }
    }
}
