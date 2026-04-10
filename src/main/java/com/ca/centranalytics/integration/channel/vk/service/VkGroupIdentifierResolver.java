package com.ca.centranalytics.integration.channel.vk.service;

import com.ca.centranalytics.integration.channel.vk.domain.VkGroupCandidate;
import com.ca.centranalytics.integration.channel.vk.repository.VkGroupCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VkGroupIdentifierResolver {
    private static final Pattern VK_ALIAS_PATTERN = Pattern.compile("^(?:club|public)(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIGITS_PATTERN = Pattern.compile("^\\d+$");

    private final VkGroupCandidateRepository vkGroupCandidateRepository;

    public VkResolvedGroupSelection resolve(List<String> groupIdentifiers) {
        Map<Long, VkGroupCandidate> resolvedById = new LinkedHashMap<>();
        Set<String> unresolved = new LinkedHashSet<>();

        for (String rawIdentifier : groupIdentifiers) {
            String normalized = normalize(rawIdentifier);
            if (normalized == null) {
                unresolved.add(rawIdentifier);
                continue;
            }

            Optional<VkGroupCandidate> resolved = resolveSingle(normalized);
            if (resolved.isPresent()) {
                resolvedById.putIfAbsent(resolved.get().getId(), resolved.get());
            } else {
                unresolved.add(rawIdentifier);
            }
        }

        return new VkResolvedGroupSelection(List.copyOf(resolvedById.values()), List.copyOf(unresolved));
    }

    private Optional<VkGroupCandidate> resolveSingle(String identifier) {
        if (DIGITS_PATTERN.matcher(identifier).matches()) {
            Long numericId = Long.parseLong(identifier);
            Optional<VkGroupCandidate> byCandidateId = vkGroupCandidateRepository.findById(numericId);
            if (byCandidateId.isPresent()) {
                return byCandidateId;
            }
            return vkGroupCandidateRepository.findByVkGroupId(numericId);
        }

        Matcher aliasMatcher = VK_ALIAS_PATTERN.matcher(identifier);
        if (aliasMatcher.matches()) {
            return vkGroupCandidateRepository.findByVkGroupId(Long.parseLong(aliasMatcher.group(1)));
        }

        return vkGroupCandidateRepository.findByScreenNameIgnoreCase(identifier);
    }

    private String normalize(String rawIdentifier) {
        if (rawIdentifier == null) {
            return null;
        }

        String trimmed = rawIdentifier.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String unwrapped = unwrapUrl(trimmed);
        if (unwrapped.startsWith("@")) {
            unwrapped = unwrapped.substring(1);
        }
        return unwrapped.trim().toLowerCase(Locale.ROOT);
    }

    private String unwrapUrl(String value) {
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return value;
        }

        URI uri = URI.create(value);
        String host = uri.getHost();
        if (host == null || !host.toLowerCase(Locale.ROOT).contains("vk.com")) {
            return value;
        }

        String path = uri.getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return value;
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
