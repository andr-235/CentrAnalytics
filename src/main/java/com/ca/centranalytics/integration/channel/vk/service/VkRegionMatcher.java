package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkMatchSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
public class VkRegionMatcher {

    public Optional<VkMatchSource> match(String requestedRegion, String structuredRegion, String searchableText) {
        if (!StringUtils.hasText(requestedRegion)) {
            return Optional.empty();
        }

        String normalizedRegion = normalize(requestedRegion);
        if (containsNormalized(structuredRegion, normalizedRegion)) {
            return Optional.of(VkMatchSource.STRUCTURED);
        }
        if (containsNormalized(searchableText, normalizedRegion)) {
            return Optional.of(VkMatchSource.TEXT);
        }
        return Optional.empty();
    }

    private boolean containsNormalized(String value, String normalizedRegion) {
        return StringUtils.hasText(value) && normalize(value).contains(normalizedRegion);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
    }
}
