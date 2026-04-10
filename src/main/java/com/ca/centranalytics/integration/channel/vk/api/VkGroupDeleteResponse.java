package com.ca.centranalytics.integration.channel.vk.api;

import java.util.List;

public record VkGroupDeleteResponse(
        List<VkGroupCandidateResponse> deletedGroups,
        List<String> unresolvedIdentifiers
) {
}
