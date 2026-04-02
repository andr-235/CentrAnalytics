package com.ca.centranalytics.integration.ingestion.service;

import com.ca.centranalytics.integration.domain.entity.ExternalUser;
import com.ca.centranalytics.integration.domain.entity.Platform;
import com.ca.centranalytics.integration.domain.repository.ExternalUserRepository;
import com.ca.centranalytics.integration.ingestion.dto.InboundAuthor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalUserResolver {

    private final ExternalUserRepository externalUserRepository;

    public ExternalUser resolve(Platform platform, InboundAuthor inboundAuthor) {
        if (inboundAuthor == null) {
            return null;
        }

        return externalUserRepository.findByPlatformAndExternalUserId(platform, inboundAuthor.externalUserId())
                .map(existing -> {
                    existing.setDisplayName(inboundAuthor.displayName());
                    existing.setUsername(inboundAuthor.username());
                    existing.setFirstName(inboundAuthor.firstName());
                    existing.setLastName(inboundAuthor.lastName());
                    existing.setPhone(inboundAuthor.phone());
                    existing.setProfileUrl(inboundAuthor.profileUrl());
                    existing.setBot(inboundAuthor.bot());
                    existing.setMetadataJson(inboundAuthor.metadataJson());
                    return externalUserRepository.save(existing);
                })
                .orElseGet(() -> externalUserRepository.save(ExternalUser.builder()
                        .platform(platform)
                        .externalUserId(inboundAuthor.externalUserId())
                        .displayName(inboundAuthor.displayName())
                        .username(inboundAuthor.username())
                        .firstName(inboundAuthor.firstName())
                        .lastName(inboundAuthor.lastName())
                        .phone(inboundAuthor.phone())
                        .profileUrl(inboundAuthor.profileUrl())
                        .bot(inboundAuthor.bot())
                        .metadataJson(inboundAuthor.metadataJson())
                        .build()));
    }
}
