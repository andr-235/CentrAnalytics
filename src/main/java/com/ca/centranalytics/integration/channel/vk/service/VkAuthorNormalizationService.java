package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkUserCandidate;
import com.ca.centranalytics.integration.ingestion.dto.InboundAuthor;
import org.springframework.stereotype.Service;

@Service
public class VkAuthorNormalizationService {

    public InboundAuthor toInboundAuthor(VkUserCandidate candidate) {
        if (candidate == null) {
            return null;
        }

        return new InboundAuthor(
                String.valueOf(candidate.getVkUserId()),
                candidate.getDisplayName(),
                null,
                candidate.getFirstName(),
                candidate.getLastName(),
                null,
                candidate.getProfileUrl(),
                false,
                candidate.getRawJson()
        );
    }
}
