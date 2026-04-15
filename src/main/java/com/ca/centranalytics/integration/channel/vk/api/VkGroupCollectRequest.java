package com.ca.centranalytics.integration.channel.vk.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record VkGroupCollectRequest(
        @NotEmpty(message = "groupIdentifiers must not be empty")
        List<String> groupIdentifiers,
        Integer postLimit,
        Integer commentPostLimit,
        Integer commentLimit
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
}
