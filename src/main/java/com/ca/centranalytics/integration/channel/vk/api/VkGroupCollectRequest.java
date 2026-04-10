package com.ca.centranalytics.integration.channel.vk.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record VkGroupCollectRequest(
        @NotEmpty(message = "groupIdentifiers must not be empty")
        List<String> groupIdentifiers,
        Integer postLimit,
        Integer commentPostLimit,
        Integer commentLimit,
        @Pattern(regexp = "OFFICIAL_ONLY|HYBRID", message = "collectionMode must be OFFICIAL_ONLY or HYBRID")
        String collectionMode
) {
    public int resolvedPostLimit() {
        return postLimit == null ? 25 : postLimit;
    }

    public int resolvedCommentPostLimit() {
        return commentPostLimit == null ? 5 : commentPostLimit;
    }

    public int resolvedCommentLimit() {
        return commentLimit == null ? 25 : commentLimit;
    }

    public String resolvedCollectionMode() {
        return collectionMode == null || collectionMode.isBlank() ? "HYBRID" : collectionMode;
    }
}
