package com.ca.centranalytics.integration.channel.vk;

import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import com.ca.centranalytics.integration.channel.vk.service.VkRegionMatcher;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VkRegionMatcherTest {

    private final VkRegionMatcher vkRegionMatcher = new VkRegionMatcher();

    @Test
    void prefersStructuredRegionMatch() {
        Optional<VkMatchSource> match = vkRegionMatcher.match("Primorsky Krai", "Vladivostok, Primorsky Krai", "Community for the Far East");

        assertThat(match).contains(VkMatchSource.STRUCTURED);
    }

    @Test
    void fallsBackToTextMatchWhenStructuredFieldDoesNotMatch() {
        Optional<VkMatchSource> match = vkRegionMatcher.match("Primorsky Krai", "Moscow", "News from Primorsky Krai");

        assertThat(match).contains(VkMatchSource.TEXT);
    }

    @Test
    void returnsEmptyWhenRegionIsNotFound() {
        Optional<VkMatchSource> match = vkRegionMatcher.match("Primorsky Krai", "Moscow", "Community for Saint Petersburg");

        assertThat(match).isEmpty();
    }
}
