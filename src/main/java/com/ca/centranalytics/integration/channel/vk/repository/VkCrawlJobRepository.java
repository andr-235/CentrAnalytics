package com.ca.centranalytics.integration.channel.vk.repository;

import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJob;
import com.ca.centranalytics.integration.channel.vk.domain.VkCrawlJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VkCrawlJobRepository extends JpaRepository<VkCrawlJob, Long> {
    List<VkCrawlJob> findByStatus(VkCrawlJobStatus status);
}
