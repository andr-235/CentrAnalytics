package com.ca.centranalytics.integration.channel.vk.api;

import java.util.List;

public record VkGroupCollectResponse(
        List<VkGroupCandidateResponse> resolvedGroups,
        List<String> unresolvedIdentifiers,
        List<VkCrawlJobResponse> postJobs,
        List<VkCrawlJobResponse> commentJobs
) {
}
