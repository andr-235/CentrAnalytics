package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.service.VkFallbackPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VkFallbackPolicyTest {

    private final VkFallbackPolicy vkFallbackPolicy = new VkFallbackPolicy();

    @Test
    void allowsFallbackOnlyForHybridMode() {
        assertThat(vkFallbackPolicy.isFallbackAllowed("HYBRID")).isTrue();
        assertThat(vkFallbackPolicy.isFallbackAllowed("OFFICIAL_ONLY")).isFalse();
        assertThat(vkFallbackPolicy.isFallbackAllowed(null)).isFalse();
    }

    @Test
    void requestsFallbackWhenOfficialSearchIsEmptyInHybridMode() {
        assertThat(vkFallbackPolicy.shouldFallbackForSearch("HYBRID", true, false)).isTrue();
        assertThat(vkFallbackPolicy.shouldFallbackForSearch("OFFICIAL_ONLY", true, false)).isFalse();
    }

    @Test
    void requestsFallbackWhenOfficialDataIsIncompleteInHybridMode() {
        assertThat(vkFallbackPolicy.shouldFallbackForSearch("HYBRID", false, true)).isTrue();
        assertThat(vkFallbackPolicy.shouldFallbackForSearch("HYBRID", false, false)).isFalse();
    }
}
