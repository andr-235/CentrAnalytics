package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;

import java.util.List;

public record VkResolvedGroupSelection(
        List<VkGroupCandidate> resolvedGroups,
        List<String> unresolvedIdentifiers
) {
}
